package com.simhastha.service;
import static org.junit.jupiter.api.Assertions.*; import com.simhastha.model.Ghat; import java.util.*; import org.junit.jupiter.api.Test;
class GhatCatalogueServiceTest {
 @Test void bootstrapCatalogueContainsSeparateTrimbakeshwarContext(){var c=new GhatCatalogueService(); assertTrue(c.catalogue().stream().anyMatch(g->g.name().equals("Ramkund"))); Ghat k=c.catalogue().stream().filter(g->g.id().equals("kushavart")).findFirst().orElseThrow(); assertEquals(GhatCatalogueService.Region.TRIMBAKESHWAR,c.regionOf(k)); assertEquals(Ghat.CrowdLevel.UNKNOWN,k.crowdLevel());}
 @Test void catalogueAssignsOnlyNamedVerifiedImageSources(){var c=new GhatCatalogueService(); Ghat ramkund=c.catalogue().stream().filter(g->g.id().equals("ramkund")).findFirst().orElseThrow(); Ghat tapovan=c.catalogue().stream().filter(g->g.id().equals("tapovan")).findFirst().orElseThrow(); Ghat kushavart=c.catalogue().stream().filter(g->g.id().equals("kushavart")).findFirst().orElseThrow(); assertEquals("/images/ramkund_sunrise.jpg",ramkund.imageUrl()); assertTrue(tapovan.imageUrl().contains("cdn.s3waas.gov.in")); assertTrue(kushavart.imageUrl().contains("cdn.s3waas.gov.in")); assertTrue(c.catalogue().stream().filter(g->!java.util.Set.of("ramkund","tapovan","kushavart").contains(g.id())).allMatch(g->g.imageUrl().isBlank()));}
 @Test void unknownCrowdSortsAfterLiveLevels(){Ghat low=new Ghat("l","l","", "",null,null,null,null,"",null,Ghat.CrowdLevel.LOW,null,false,null,List.of(),null,null,"",null); Ghat unknown=new Ghat("u","u","", "",null,null,null,null,"",null,Ghat.CrowdLevel.UNKNOWN,null,false,null,List.of(),null,null,"",null); assertEquals("l",List.of(unknown,low).stream().sorted(GhatCrowdComparator.LIVE_CROWD_ORDER).findFirst().orElseThrow().id());}
 @Test void weatherWithoutProviderIsUnavailable(){assertFalse(new WeatherService((lat, lon) -> Optional.empty()).weather(20d,73d).available());}
 @Test void weatherUsesEachGhatCoordinatesAndCachesByLocation(){
  java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
  WeatherService weather=new WeatherService((lat,lon)->{calls.incrementAndGet();return Optional.of(new Ghat.Weather((int)Math.round(lat),"Clear",20,68,"now"));});
  Ghat ramkund=new Ghat("ramkund","Ramkund","Panchavati, Nashik","",20.0059,73.7890,null,null,"",null,Ghat.CrowdLevel.UNKNOWN,null,false,null,List.of(),null,null,"",null);
  Ghat tapovan=new Ghat("tapovan","Tapovan Ghat","Tapovan, Nashik","",20.0132,73.8080,null,null,"",null,Ghat.CrowdLevel.UNKNOWN,null,false,null,List.of(),null,null,"",null);
  assertEquals(20,weather.weatherFor(ramkund).current().temperatureCelsius());
  assertEquals(20,weather.weatherFor(tapovan).current().temperatureCelsius());
  assertEquals(2,calls.get());
 weather.weatherFor(ramkund); assertEquals(2,calls.get());
 }
 @Test void allCatalogueGhatsRequestWeatherUsingTheirOwnLocations(){
  java.util.Set<String> coordinateKeys=new java.util.HashSet<>();
  WeatherService weather=new WeatherService((lat,lon)->{coordinateKeys.add(lat+":"+lon);return Optional.of(new Ghat.Weather(27,"Clear",null,60,"now"));});
  List<Ghat> ghats=new GhatCatalogueService().catalogue();
  ghats.forEach(ghat->assertTrue(weather.weatherFor(ghat).current().available(),ghat.name()));
  assertEquals(ghats.size(),coordinateKeys.size());
 }
 @Test void guideHasNoInventedEvents(){assertTrue(new SnanGuideService().publishedEvents().isEmpty());}
}
