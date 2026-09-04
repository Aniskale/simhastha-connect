package com.simhastha.view;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Exercises the actual DashboardPage ghat render path with no live Firebase records. */
class DashboardPageGhatRenderingTest {
    private static final AtomicReference<Throwable> startupError = new AtomicReference<>();

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
        } catch (IllegalStateException alreadyStarted) {
            started.countDown();
        }
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
    }

    @Test
    void emptyLiveBackendAddsCatalogueCardsToTheVisibleGhatFlowPane() throws Exception {
        String original = System.getProperty("firebase.enabled");
        System.setProperty("firebase.enabled", "false");
        startupError.set(null);
        CountDownLatch rendered = new CountDownLatch(1);
        AtomicReference<FlowPane> flowPane = new AtomicReference<>();
        try {
            Platform.runLater(() -> {
                try {
                    DashboardPage page = new DashboardPage();
                    Method pageMethod = DashboardPage.class.getDeclaredMethod("ghatsPage");
                    pageMethod.setAccessible(true);
                    VBox content = (VBox) pageMethod.invoke(page);
                    flowPane.set(findGhatFlowPane(content));
                    Thread waiter = new Thread(() -> {
                        try { Thread.sleep(750); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                        Platform.runLater(() -> {
                            try {
                                assertTrue(flowPane.get().getChildren().size() >= 15, "Ghat FlowPane should receive catalogue cards");
                            } catch (Throwable error) {
                                startupError.set(error);
                            } finally {
                                rendered.countDown();
                            }
                        });
                    }, "ghat-render-test-waiter");
                    waiter.setDaemon(true);
                    waiter.start();
                } catch (Throwable error) {
                    startupError.set(error);
                    rendered.countDown();
                }
            });
            assertTrue(rendered.await(10, TimeUnit.SECONDS), "Ghat cards did not render");
            if (startupError.get() != null) throw new AssertionError(startupError.get());
            assertTrue(flowPane.get().getChildren().size() >= 15, "Expected every catalogue Ghat to render");
        } finally {
            if (original == null) System.clearProperty("firebase.enabled"); else System.setProperty("firebase.enabled", original);
        }
    }

    @Test
    void heroKeepsTheLightControlsLayerCompactAboveTheBackgroundImage() throws Exception {
        CountDownLatch inspected = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                DashboardPage page = new DashboardPage();
                Method heroMethod = DashboardPage.class.getDeclaredMethod("ghatHero");
                heroMethod.setAccessible(true);
                StackPane hero = (StackPane) heroMethod.invoke(page);
                assertEquals(4, hero.getChildren().size());
                assertTrue(hero.getChildren().get(0) instanceof ImageView);
                assertTrue(hero.getChildren().get(1).getStyleClass().contains("ghat-hero-overlay"));
                HBox controls = (HBox) hero.getChildren().get(3);
                assertTrue(controls.getStyleClass().contains("ghat-top-controls"));
                assertEquals(Region.USE_PREF_SIZE, controls.getMaxWidth());
                assertEquals(Region.USE_PREF_SIZE, controls.getMaxHeight());
                Scene scene = new Scene(hero, 1200, 196);
                var css = DashboardPage.class.getResource("/css/simhastha-theme.css");
                scene.getStylesheets().add(css.toExternalForm());
                hero.applyCss();
                hero.layout();
                assertTrue(controls.getWidth() < hero.getWidth(), "Controls background must not cover the hero");
                assertTrue(controls.getBackground().getFills().stream().allMatch(fill ->
                        !(fill.getFill() instanceof Color color) || color.getOpacity() == 0),
                        "Hero controls must not create an opaque light overlay");
            } catch (Throwable error) {
                startupError.set(error);
            } finally {
                inspected.countDown();
            }
        });
        assertTrue(inspected.await(10, TimeUnit.SECONDS), "Hero was not inspected");
        if (startupError.get() != null) throw new AssertionError(startupError.get());
    }

    private FlowPane findGhatFlowPane(VBox content) {
        for (Node child : content.getChildren()) {
            if (child instanceof FlowPane flow && flow.getStyleClass().contains("ghat-card-grid")) return flow;
        }
        throw new AssertionError("Visible Ghats FlowPane was not attached to the page");
    }
}
