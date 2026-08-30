package com.simhastha.service;

import static org.junit.jupiter.api.Assertions.*;
import com.simhastha.model.Ghat;
import com.simhastha.model.GhatOperationalState;
import java.util.List;
import org.junit.jupiter.api.Test;

class GhatNavigationServiceTest {
 @Test void choosesActiveEntryOnlyGateAndRejectsUnsafeGhat() {
  Ghat ghat = ghat(Ghat.OperationalStatus.OPEN, Ghat.CrowdLevel.LOW, new GhatOperationalState(GhatOperationalState.BathingStatus.AVAILABLE, GhatOperationalState.WaterSafety.NORMAL, List.of(), List.of(), List.of(new GhatOperationalState.Gate("exit","Exit", GhatOperationalState.GateStatus.EXIT_ONLY,"",20d,73d),new GhatOperationalState.Gate("entry","Entry", GhatOperationalState.GateStatus.ENTRY_ONLY,"",21d,74d)), List.of(),List.of(),GhatOperationalState.CleaningStatus.NORMAL,"",GhatOperationalState.PriorityAlert.none(),""));
  assertEquals("Entry", new GhatNavigationService().destinationFor(ghat).orElseThrow().entryName());
  assertEquals(GhatNavigationService.Decision.UNSAFE, new GhatNavigationService().decision(ghat(Ghat.OperationalStatus.EMERGENCY_CLOSED, Ghat.CrowdLevel.LOW, ghat.operationalState())));
 }
 @Test void corridorShowsOnlyApprovedActiveBusinessesNearGeometry() {
  RouteBusinessService service = new RouteBusinessService(); var route=List.of(new GhatNavigationService.Point(20,73),new GhatNavigationService.Point(20,73.01));
  var result=service.nearRoute(route,List.of(new RouteBusinessService.BusinessPoint("ok","Food","Food",20.0005,73.005,true,true),new RouteBusinessService.BusinessPoint("far","Far","Food",20.02,73.005,true,true),new RouteBusinessService.BusinessPoint("pending","Pending","Food",20,73.005,false,true)),500);
  assertEquals(List.of("ok"),result.stream().map(RouteBusinessService.BusinessPoint::id).toList());
 }
 private Ghat ghat(Ghat.OperationalStatus status,Ghat.CrowdLevel crowd,GhatOperationalState state){return new Ghat("g","G","", "",20d,73d,null,null,"",status,crowd,10,true,new Ghat.Walking(Ghat.WalkingDifficulty.EASY,0,0,true,true),List.of(),Ghat.Weather.unavailable(),Ghat.History.unavailable(),"",state);}
}
