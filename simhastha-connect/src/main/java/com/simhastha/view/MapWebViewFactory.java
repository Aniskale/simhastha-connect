package com.simhastha.view;

import com.simhastha.model.BusinessLocation;
import com.simhastha.model.MapLocation;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public final class MapWebViewFactory {
    private static final Logger LOGGER = Logger.getLogger(MapWebViewFactory.class.getName());
    private static final double DEFAULT_LAT = 20.0110;
    private static final double DEFAULT_LON = 73.7900;
    private static final TileProvider TILE_PROVIDER = new TileProvider(
            "OpenStreetMap DE",
            "https://tile.openstreetmap.de/{z}/{x}/{y}.png",
            "https://tile.openstreetmap.de/%d/%d/%d.png",
            "&copy; OpenStreetMap contributors",
            "Tiles © OpenStreetMap",
            19,
            false);

    private MapWebViewFactory() {
    }

    public static Node businessSelector(BusinessLocation initial, BiConsumer<Double, Double> onPick,
            Runnable onLoaded, Runnable onFailed) {
        long startedAt = System.nanoTime();
        double lat = parse(initial == null ? "" : initial.latitude(), DEFAULT_LAT);
        double lon = parse(initial == null ? "" : initial.longitude(), DEFAULT_LON);
        LOGGER.info("Business location selector WebView load started at " + number(lat) + ", " + number(lon));
        WebView webView = webView();
        WebEngine engine = webView.getEngine();
        java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean();
        attachDiagnostics(engine, "Business location selector");
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                LOGGER.info("Business location selector WebView load succeeded in " + elapsedMillis(startedAt) + " ms.");
                JSObject window = (JSObject) engine.executeScript("window");
                SelectorBridge bridge = new SelectorBridge(onPick,
                        () -> { completed.set(true); if (onLoaded != null) onLoaded.run(); },
                        () -> { completed.set(true); if (onFailed != null) onFailed.run(); }, startedAt);
                window.setMember("javaBridge", bridge);
                notifyExistingState(engine, bridge::mapReady, bridge::mapFailed);
                if (scriptBoolean(engine, "window.__firstTileVisible === true")) bridge.mapFirstTileVisible();
            } else if (state == Worker.State.FAILED || state == Worker.State.CANCELLED) {
                LOGGER.log(Level.WARNING, "Business location selector map failed to load.",
                        engine.getLoadWorker().getException());
                if (onFailed != null) Platform.runLater(onFailed);
            }
        });
        LOGGER.info("Business location selector map HTML load started.");
        engine.loadContent(selectorHtml(lat, lon));
        mapWatchdog(completed, engine, onFailed, "Business location selector");
        return shell(webView);
    }

    /** Recalculates the existing Leaflet viewport after the selector dialog has its final size. */
    public static void invalidateBusinessSelectorMap(Node selector) {
        if (selector == null) return;
        Node candidate = selector instanceof WebView ? selector : selector.lookup(".web-view");
        if (!(candidate instanceof WebView webView)) {
            LOGGER.warning("Business location selector WebView was unavailable for resizing.");
            return;
        }
        invalidateMap(webView.getEngine(), "Business location selector");
    }

    public static Node businessMarkerMap(List<MapLocation> locations, String selectedBusinessId,
            boolean routeMode, MarkerActionHandler handler, Runnable onLoaded, Runnable onFailed) {
        WebView webView = webView();
        WebEngine engine = webView.getEngine();
        java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean();
        attachDiagnostics(engine, "Simhastha business marker map");
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                MarkerBridge bridge = new MarkerBridge(handler,
                        () -> { completed.set(true); if (onLoaded != null) onLoaded.run(); },
                        () -> { completed.set(true); if (onFailed != null) onFailed.run(); });
                window.setMember("javaBridge", bridge);
                notifyExistingState(engine, bridge::mapReady, bridge::mapFailed);
            } else if (state == Worker.State.FAILED || state == Worker.State.CANCELLED) {
                LOGGER.log(Level.WARNING, "Simhastha business marker map failed to load.",
                        engine.getLoadWorker().getException());
                if (onFailed != null) Platform.runLater(onFailed);
            }
        });
        engine.loadContent(markerHtml(locations, selectedBusinessId == null ? "" : selectedBusinessId, routeMode));
        mapWatchdog(completed, engine, onFailed, "Simhastha business marker map");
        return shell(webView);
    }

    private static void mapWatchdog(java.util.concurrent.atomic.AtomicBoolean completed, WebEngine engine, Runnable onFailed,
            String mapName) {
        PauseTransition timeout = new PauseTransition(Duration.seconds(12));
        timeout.setOnFinished(event -> {
            if (completed.get()) return;
            if (scriptBoolean(engine, "window.__mapReady === true")) {
                completed.set(true);
                try {
                    engine.executeScript("if(document.getElementById('loading'))document.getElementById('loading').style.display='none';");
                } catch (RuntimeException exception) {
                    LOGGER.log(Level.FINE, mapName + " loading overlay could not be hidden.", exception);
                }
                return;
            }
            if (scriptBoolean(engine, "window.__mapFailed === true")) {
                completed.set(true);
                if (onFailed != null) onFailed.run();
                return;
            }
            if (scriptBoolean(engine, "!!window.map")) {
                completed.set(true);
                try {
                    engine.executeScript("window.__mapReady=true;if(document.getElementById('loading'))document.getElementById('loading').style.display='none';if(window.map&&window.map.invalidateSize)window.map.invalidateSize();");
                } catch (RuntimeException exception) {
                    LOGGER.log(Level.FINE, mapName + " displayed but final ready script could not run.", exception);
                }
                return;
            }
            LOGGER.fine(mapName + " did not complete JavaScript initialization before the timeout.");
            completed.set(true);
            if (onFailed != null) onFailed.run();
        });
        timeout.play();
    }

    /** Displays a static tile map when the JavaFX WebView cannot trust the Leaflet CDN certificate. */
    public static Node nativeBusinessMap(List<MapLocation> locations, String selectedBusinessId) {
        List<MapLocation> visible = locations == null ? List.of() : locations;
        MapLocation center = visible.stream()
                .filter(location -> location != null && selectedBusinessId != null
                        && selectedBusinessId.equals(location.businessId()))
                .findFirst()
                .orElse(visible.isEmpty() ? null : visible.get(0));
        double latitude = center == null ? DEFAULT_LAT : center.latitude();
        double longitude = center == null ? DEFAULT_LON : center.longitude();
        int zoom = 13;
        int centerX = lonToTileX(longitude, zoom);
        int centerY = latToTileY(latitude, zoom);
        GridPane tiles = new GridPane();
        for (int row = -1; row <= 1; row++) {
            for (int column = -1; column <= 1; column++) {
                int tileX = centerX + column;
                int tileY = centerY + row;
                ImageView tile = new ImageView(new Image(tileUrl(zoom, tileX, tileY), true));
                tile.setFitWidth(256);
                tile.setFitHeight(256);
                tile.setPreserveRatio(false);
                tiles.add(tile, column + 1, row + 1);
            }
        }
        Label attribution = new Label(TILE_PROVIDER.shortAttribution());
        attribution.setStyle("-fx-background-color:rgba(255,255,255,.82);-fx-text-fill:#5f4631;-fx-font-size:10px;"
                + "-fx-padding:2px 5px;");
        StackPane map = new StackPane(tiles, attribution);
        StackPane.setAlignment(attribution, javafx.geometry.Pos.BOTTOM_RIGHT);
        map.setMinHeight(300);
        map.setPrefHeight(420);
        map.getStyleClass().add("simhastha-map-canvas");
        for (MapLocation location : visible) {
            Label marker = new Label("\uE707");
            marker.setStyle("-fx-text-fill:#8f0f13;-fx-font-size:24px;-fx-font-weight:bold;");
            marker.setTooltip(new javafx.scene.control.Tooltip(location.name()));
            StackPane.setAlignment(marker, javafx.geometry.Pos.CENTER);
            map.getChildren().add(marker);
        }
        return map;
    }

    private static String tileUrl(int zoom, int x, int y) {
        int count = 1 << zoom;
        int wrappedX = ((x % count) + count) % count;
        return TILE_PROVIDER.nativeTileUrl(zoom, wrappedX, y);
    }

    private static int lonToTileX(double longitude, int zoom) {
        return (int) Math.floor((longitude + 180.0) / 360.0 * (1 << zoom));
    }

    private static int latToTileY(double latitude, int zoom) {
        double radians = Math.toRadians(latitude);
        return (int) Math.floor((1.0 - Math.log(Math.tan(radians) + 1.0 / Math.cos(radians)) / Math.PI)
                / 2.0 * (1 << zoom));
    }

    public interface MarkerActionHandler {
        void markerSelected(String businessId);
        void viewBusiness(String businessId);
        void getRoute(String businessId);
    }

    public static final class SelectorBridge {
        private final BiConsumer<Double, Double> onPick;
        private final Runnable onLoaded;
        private final Runnable onFailed;
        private final long startedAt;
        private final java.util.concurrent.atomic.AtomicBoolean firstTileLogged = new java.util.concurrent.atomic.AtomicBoolean();

        SelectorBridge(BiConsumer<Double, Double> onPick, Runnable onLoaded, Runnable onFailed, long startedAt) {
            this.onPick = onPick;
            this.onLoaded = onLoaded;
            this.onFailed = onFailed;
            this.startedAt = startedAt;
        }

        public void mapClicked(double latitude, double longitude) {
            if (onPick != null) Platform.runLater(() -> onPick.accept(latitude, longitude));
        }

        public void mapReady() {
            LOGGER.info("Business location selector Leaflet map ready in " + elapsedMillis(startedAt) + " ms.");
            if (onLoaded != null) Platform.runLater(onLoaded);
        }

        public void mapFirstTileVisible() {
            if (firstTileLogged.compareAndSet(false, true)) {
                LOGGER.info("Business location selector first tiles visible in " + elapsedMillis(startedAt) + " ms.");
            }
        }

        public void mapFailed() {
            LOGGER.warning("Business location selector JavaScript map reported failure.");
            if (onFailed != null) Platform.runLater(onFailed);
        }

        public void mapTileFailed(String provider) {
            LOGGER.warning("Business location selector tile failed from configured provider: " + provider);
        }
    }

    public static final class MarkerBridge {
        private final MarkerActionHandler handler;
        private final Runnable onLoaded;
        private final Runnable onFailed;

        MarkerBridge(MarkerActionHandler handler, Runnable onLoaded, Runnable onFailed) {
            this.handler = handler;
            this.onLoaded = onLoaded;
            this.onFailed = onFailed;
        }

        public void markerSelected(String businessId) {
            if (handler != null) Platform.runLater(() -> handler.markerSelected(businessId));
        }

        public void viewBusiness(String businessId) {
            if (handler != null) Platform.runLater(() -> handler.viewBusiness(businessId));
        }

        public void getRoute(String businessId) {
            if (handler != null) Platform.runLater(() -> handler.getRoute(businessId));
        }

        public void mapReady() {
            if (onLoaded != null) Platform.runLater(onLoaded);
        }

        public void mapFailed() {
            if (onFailed != null) Platform.runLater(onFailed);
        }

        public void mapTileFailed(String provider) {
            LOGGER.warning("Simhastha business marker map tile failed from configured provider: " + provider);
        }
    }

    private static WebView webView() {
        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.getEngine().setJavaScriptEnabled(true);
        LOGGER.info("JavaFX WebView initialized; JavaScript is enabled.");
        webView.setMinWidth(0);
        webView.setPrefWidth(780);
        webView.setMaxWidth(Double.MAX_VALUE);
        webView.setMinHeight(300);
        webView.setPrefHeight(420);
        webView.setMaxHeight(Double.MAX_VALUE);
        return webView;
    }

    private static StackPane shell(WebView webView) {
        StackPane shell = new StackPane(webView);
        shell.setMinHeight(300);
        shell.setPrefHeight(420);
        shell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        shell.getStyleClass().add("simhastha-map-web-shell");
        shell.sceneProperty().addListener((observable, oldScene, scene) -> {
            if (scene != null) {
                Platform.runLater(() -> {
                    try {
                        invalidateMap(webView.getEngine(), "Map");
                    } catch (RuntimeException exception) {
                        LOGGER.log(Level.FINE, "Map resize notification could not be sent.", exception);
                    }
                });
            }
        });
        return shell;
    }

    private static void invalidateMap(WebEngine engine, String mapName) {
        try {
            Object resized = engine.executeScript("(function(){if(!window.map||!window.map.invalidateSize)return false;"
                    + "window.dispatchEvent(new Event('resize'));window.map.invalidateSize(true);return true;})()");
            if (Boolean.TRUE.equals(resized)) LOGGER.info(mapName + " map invalidated/resized.");
        } catch (RuntimeException exception) {
            LOGGER.log(Level.FINE, mapName + " resize notification could not be sent.", exception);
        }
    }

    private static void attachDiagnostics(WebEngine engine, String label) {
        engine.setOnError(event -> LOGGER.log(Level.WARNING,
                label + " WebView error: " + event.getMessage(), event.getException()));
        engine.getLoadWorker().exceptionProperty().addListener((observable, oldException, exception) -> {
            if (exception != null) LOGGER.log(Level.WARNING, label + " WebEngine load exception.", exception);
        });
    }

    private static void notifyExistingState(WebEngine engine, Runnable onReady, Runnable onFailed) {
        try {
            if (scriptBoolean(engine, "window.__mapReady === true")) {
                // The page may finish booting before the Java bridge is attached.
                // Hide the in-page overlay as soon as the completed state is observed.
                engine.executeScript("if(window.readyMap)readyMap();else if(document.getElementById('loading'))document.getElementById('loading').style.display='none';");
                onReady.run();
            } else if (scriptBoolean(engine, "window.__mapFailed === true")) {
                onFailed.run();
            }
        } catch (Exception exception) {
            LOGGER.log(Level.FINE, "Unable to inspect the completed map JavaScript state.", exception);
        }
    }

    private static boolean scriptBoolean(WebEngine engine, String script) {
        try {
            return Boolean.TRUE.equals(engine.executeScript(script));
        } catch (Exception exception) {
            return false;
        }
    }

    private static String selectorHtml(double latitude, double longitude) {
        return (selectorHead() + """
                <body><style>html,body{width:100%;height:100%;margin:0;padding:0;overflow:hidden}body{position:relative}#map{position:absolute;inset:0;width:100%;height:100%;margin:0;padding:0}</style><div id="map"></div><div id="loading">Loading map...</div><div id="error">Map could not be loaded. Check map/network configuration.</div>
                <script>
                window.onerror=function(message,source,line,column,error){console.error('Map JavaScript error: '+message+' @ '+line+':'+column);failMap();return true;};
                var marker;
                var tileProvider=TILE_PROVIDER_JSON;
                function failMap(){window.__mapFailed=true;document.getElementById('loading').style.display='none';document.getElementById('error').style.display='flex';if(window.javaBridge)window.javaBridge.mapFailed();}
                function readyMap(){window.__mapReady=true;document.getElementById('loading').style.display='none';if(window.javaBridge)window.javaBridge.mapReady();}
                function firstTileVisible(){if(window.__firstTileVisible)return;window.__firstTileVisible=true;if(window.javaBridge)window.javaBridge.mapFirstTileVisible();}
                function attachTiles(map){var layer=L.tileLayer(tileProvider.url,{maxZoom:tileProvider.maxZoom,attribution:tileProvider.attribution,keepBuffer:0,updateWhenIdle:true,fadeAnimation:false,zoomAnimation:false});layer.once('tileload',firstTileVisible);layer.on('tileerror',function(e){console.error('Map tile failed from '+tileProvider.name+'. '+(e&&e.coords?JSON.stringify(e.coords):''));if(window.javaBridge)window.javaBridge.mapTileFailed(tileProvider.name);});layer.addTo(map);return layer;}
                function boot(){try{if(window.__mapBooted)return;window.__mapBooted=true;if(!window.L){failMap();return;}
                  window.map=L.map('map',{zoomControl:true}).setView([SELECTOR_LAT,SELECTOR_LON],15);
                  var map=window.map;
                  attachTiles(map);
                  marker=L.marker([SELECTOR_LAT,SELECTOR_LON],{draggable:true}).addTo(map);
                  marker.on('dragend',function(e){var p=e.target.getLatLng();if(window.javaBridge)window.javaBridge.mapClicked(p.lat,p.lng);});
                  map.on('click',function(e){marker.setLatLng(e.latlng);if(window.javaBridge)window.javaBridge.mapClicked(e.latlng.lat,e.latlng.lng);});
                  requestAnimationFrame(function(){map.invalidateSize(true);readyMap();});}catch(e){console.error(e);failMap();}}
                function loadLeafletFallback(){if(window.__leafletFallbackStarted)return;window.__leafletFallbackStarted=true;var script=document.createElement('script');script.src='https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js';script.onload=boot;script.onerror=failMap;document.head.appendChild(script);}
                if(window.L){boot();}
                </script><script src="https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js" onload="boot()" onerror="loadLeafletFallback()"></script></body></html>
                """).replace("TILE_PROVIDER_JSON", tileProviderJson())
                .replace("SELECTOR_LAT", number(latitude)).replace("SELECTOR_LON", number(longitude));
    }

    private static String markerHtml(List<MapLocation> locations, String selectedBusinessId, boolean routeMode) {
        return (baseHead() + """
                <body><div id="map"></div><div id="loading">Loading map...</div><div id="error">Map could not be loaded. Check map/network configuration.</div>
                <div id="route-panel"><strong>Destination:</strong> <span id="dest">Business</span><div id="route-summary">Use Start Route for current route.</div><div class="popup-actions"><button onclick="startRoute()">Start Route</button><button onclick="recenter()">Recenter</button></div></div>
                <script>
                window.onerror=function(message,source,line,column,error){console.error('Map JavaScript error: '+message+' @ '+line+':'+column);failMap();return true;};
                var markers=MARKERS_JSON,selectedBusinessId=SELECTED_ID,routeMode=ROUTE_MODE,tileProvider=TILE_PROVIDER_JSON,map,selectedItem,selectedMarker,routeLine,startMarker,markerLookup={};
                function failMap(){window.__mapFailed=true;document.getElementById('loading').style.display='none';document.getElementById('error').style.display='flex';if(window.javaBridge)window.javaBridge.mapFailed();}
                function readyMap(){window.__mapReady=true;document.getElementById('loading').style.display='none';if(window.javaBridge)window.javaBridge.mapReady();}
                function attachTiles(map){var layer=L.tileLayer(tileProvider.url,{maxZoom:tileProvider.maxZoom,attribution:tileProvider.attribution});layer.on('tileerror',function(e){console.error('Map tile failed from '+tileProvider.name+'. '+(e&&e.coords?JSON.stringify(e.coords):''));if(window.javaBridge)window.javaBridge.mapTileFailed(tileProvider.name);});layer.addTo(map);return layer;}
                function popup(item){return '<strong>'+item.name+'</strong><br>Verified Business<br>'+item.category+'<br>'+item.location+'<div class="popup-actions"><button onclick="window.javaBridge.viewBusiness(\\''+item.businessId+'\\')">View Business</button><button onclick="selectForRoute(\\''+item.businessId+'\\')">Get Route</button></div>';}
                function selectForRoute(id){selectedItem=markerLookup[id];document.getElementById('route-panel').style.display='block';if(selectedItem)document.getElementById('dest').innerText=selectedItem.name;startRoute();}
                function recenter(){if(selectedItem)map.setView([selectedItem.lat,selectedItem.lon],16);}
                function requestExternalRoute(){try{if(window.javaBridge&&selectedItem)window.javaBridge.getRoute(selectedItem.businessId);}catch(e){}}
                function startRoute(){if(!selectedItem){routeSummary('Select a destination marker first.');return;}if(!navigator.geolocation){routeSummary('Opening route in browser for live location permission...');requestExternalRoute();return;}routeSummary('Loading route...');
                  navigator.geolocation.getCurrentPosition(function(pos){var start=[pos.coords.latitude,pos.coords.longitude],end=[selectedItem.lat,selectedItem.lon];
                    fetch('https://router.project-osrm.org/route/v1/driving/'+start[1]+','+start[0]+';'+end[1]+','+end[0]+'?overview=full&geometries=geojson').then(function(r){if(!r.ok)throw new Error();return r.json();}).then(function(data){if(!data.routes||!data.routes.length)throw new Error();var route=data.routes[0];var pts=route.geometry.coordinates.map(function(p){return[p[1],p[0]];});if(routeLine)map.removeLayer(routeLine);if(startMarker)map.removeLayer(startMarker);startMarker=L.marker(start,{title:'Current/start location'}).addTo(map);routeLine=L.polyline(pts,{color:'#8f0f13',weight:5,opacity:.82}).addTo(map);map.fitBounds(L.latLngBounds(pts).pad(.18));routeSummary('Route: '+(route.distance/1000).toFixed(1)+' km | approx '+Math.round(route.duration/60)+' min');}).catch(function(){routeSummary('Route could not be loaded. Opening browser route...');requestExternalRoute();});},function(){routeSummary('Opening route in browser for live location permission...');requestExternalRoute();},{enableHighAccuracy:true,timeout:10000,maximumAge:60000});}
                function routeSummary(text){document.getElementById('route-summary').innerText=text;}
                setTimeout(function(){if(!window.__mapReady&&!window.__leafletFallbackStarted)loadLeafletFallback();},5000);
                setTimeout(function(){if(!window.__mapReady)failMap();},10000);
                function boot(){try{if(window.__mapBooted)return;window.__mapBooted=true;if(!window.L){failMap();return;}window.map=L.map('map',{zoomControl:true}).setView([20.0110,73.7900],11);map=window.map;attachTiles(map);
                  markers.forEach(function(item){markerLookup[item.businessId]=item;var m=L.marker([item.lat,item.lon],{title:item.name}).addTo(map).bindPopup(popup(item));m.on('click',function(){selectedItem=item;selectedMarker=m;document.getElementById('dest').innerText=item.name;if(window.javaBridge)window.javaBridge.markerSelected(item.businessId);});if(item.businessId===selectedBusinessId){selectedItem=item;selectedMarker=m;}});
                  if(selectedItem){document.getElementById('dest').innerText=selectedItem.name;L.circleMarker([selectedItem.lat,selectedItem.lon],{radius:18,color:'#8f0f13',weight:3,fillColor:'#f5b33c',fillOpacity:.28}).addTo(map);map.setView([selectedItem.lat,selectedItem.lon],16);selectedMarker.openPopup();if(window.javaBridge)window.javaBridge.markerSelected(selectedItem.businessId);}else if(markers.length){map.fitBounds(L.latLngBounds(markers.map(function(i){return[i.lat,i.lon];})).pad(.25),{maxZoom:13});}
                  document.getElementById('route-panel').style.display=routeMode&&selectedItem?'block':'none';setTimeout(function(){map.invalidateSize();readyMap();},120);}catch(e){console.error(e);failMap();}}
                function loadLeafletFallback(){if(window.__leafletFallbackStarted)return;window.__leafletFallbackStarted=true;var script=document.createElement('script');script.src='https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js';script.onload=boot;script.onerror=function(){var second=document.createElement('script');second.src='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js';second.onload=boot;second.onerror=failMap;document.head.appendChild(second);};document.head.appendChild(script);}
                if(window.L){boot();}else{setTimeout(function(){if(window.L&&!window.__mapReady)boot();},800);}
                </script><script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" onload="boot()" onerror="loadLeafletFallback()"></script></body></html>
                """).replace("TILE_PROVIDER_JSON", tileProviderJson())
                .replace("MARKERS_JSON", markerJson(locations)).replace("SELECTED_ID", js(selectedBusinessId))
                .replace("ROUTE_MODE", routeMode ? "true" : "false");
    }

    private static String baseHead() {
        return """
                <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"><style>
                html,body,#map{width:100%;height:100%;margin:0;padding:0}body{overflow:hidden;font-family:Arial,sans-serif;background:#efe1c3}
                #loading,#error{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;z-index:9999;color:#7a0f12;background:rgba(255,250,240,.92);font-size:15px;font-weight:700;text-align:center;padding:20px}#error{display:none}
                #route-panel{position:absolute;left:14px;bottom:14px;z-index:9000;display:none;min-width:260px;max-width:360px;background:rgba(255,250,240,.96);border:1px solid #d7b27c;border-radius:10px;padding:12px;color:#3d1b12;box-shadow:0 8px 28px rgba(61,27,18,.16)}
                #route-summary{margin-top:6px;color:#704829;font-size:12px}.leaflet-popup-content{color:#3d1b12;font-size:13px;line-height:1.45}.popup-actions{display:flex;gap:6px;margin-top:9px}.popup-actions button{border:0;border-radius:7px;padding:7px 10px;background:#7a0f12;color:#fffaf0;font-weight:700;cursor:pointer}.popup-actions button+button{background:#fff8e9;color:#7a0f12;border:1px solid #d7b27c}
                </style></head>
                """;
    }

    private static String selectorHead() {
        return baseHead().replace("https://unpkg.com/leaflet@1.9.4/dist/leaflet.css",
                "https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css");
    }

    private static long elapsedMillis(long startedAt) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static String markerJson(List<MapLocation> locations) {
        StringBuilder json = new StringBuilder("[");
        for (MapLocation location : locations == null ? List.<MapLocation>of() : locations) {
            if (location == null || !Double.isFinite(location.latitude()) || !Double.isFinite(location.longitude())) {
                LOGGER.warning("Skipping business map marker with invalid coordinates.");
                continue;
            }
            if (json.length() > 1) json.append(',');
            json.append('{').append("\"businessId\":").append(js(location.businessId())).append(',')
                    .append("\"name\":").append(js(location.name())).append(',')
                    .append("\"category\":").append(js(location.category())).append(',')
                    .append("\"location\":").append(js(locationText(location))).append(',')
                    .append("\"lat\":").append(number(location.latitude())).append(',')
                    .append("\"lon\":").append(number(location.longitude())).append('}');
        }
        return json.append(']').toString();
    }

    private static String locationText(MapLocation location) {
        java.util.ArrayList<String> parts = new java.util.ArrayList<>();
        add(parts, location.address()); add(parts, location.area()); add(parts, location.city());
        return parts.isEmpty() ? "Address not provided" : String.join(", ", parts);
    }

    private static void add(List<String> parts, String value) {
        if (value != null && !value.isBlank() && parts.stream().noneMatch(existing -> existing.equalsIgnoreCase(value.trim()))) parts.add(value.trim());
    }

    private static double parse(String value, double fallback) {
        try { return value == null || value.isBlank() ? fallback : Double.parseDouble(value.trim()); }
        catch (Exception exception) { return fallback; }
    }

    private static String number(double value) {
        return String.format(Locale.US, "%.6f", value);
    }

    private static String js(String value) {
        String escaped = (value == null ? "" : value).replace("\\", "\\\\").replace("'", "\\'")
                .replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
        return "\"" + escaped + "\"";
    }

    private static String tileProviderJson() {
        return "{name:" + js(TILE_PROVIDER.name()) + ",url:" + js(TILE_PROVIDER.leafletUrl())
                + ",attribution:" + js(TILE_PROVIDER.attribution()) + ",maxZoom:" + TILE_PROVIDER.maxZoom() + "}";
    }

    private record TileProvider(String name, String leafletUrl, String nativeUrlPattern, String attribution,
            String shortAttribution, int maxZoom, boolean nativeYBeforeX) {
        String nativeTileUrl(int zoom, int x, int y) {
            return nativeYBeforeX
                    ? String.format(Locale.US, nativeUrlPattern, zoom, y, x)
                    : String.format(Locale.US, nativeUrlPattern, zoom, x, y);
        }
    }
}
