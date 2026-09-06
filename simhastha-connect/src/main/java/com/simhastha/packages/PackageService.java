package com.simhastha.packages;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.*;
public final class PackageService {
 private static final Executor SEARCH_EXECUTOR=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"package-catalogue-loader");t.setDaemon(true);return t;});
 /** Runs filtering and sorting away from the JavaFX application thread. */
 public CompletableFuture<List<KumbhPackage>> searchAsync(PackageSearchCriteria c,String sort){return CompletableFuture.supplyAsync(()->search(c,sort),SEARCH_EXECUTOR);}
 public List<KumbhPackage> search(PackageSearchCriteria c, String sort) {
  Stream<KumbhPackage> s=PackageRepository.packages().stream();
  if(c.origin()!=null&&!c.origin().isBlank()) s=s.filter(p->p.origin().equalsIgnoreCase(c.origin()));
  if(c.category()!=null)s=s.filter(p->p.category()==c.category());
  if(c.duration()!=null&&!c.duration().equals("Any duration"))s=s.filter(p->p.duration().equals(c.duration()));
  s=s.filter(p->match(c.types(),p.category().name())&&match(c.durations(),p.duration())&&matchBudget(c.budgets(),p.price())&&match(c.travel(),p.travel())&&match(c.stay(),p.stay())&&match(c.meals(),p.meals())&&match(c.themes(),String.join(" ",p.themes()))&&match(c.extras(),String.join(" ",p.facilities())));
  Comparator<KumbhPackage> cmp=switch(sort){case "Price Low to High"->Comparator.comparingInt(KumbhPackage::price);case "Price High to Low"->Comparator.comparingInt(KumbhPackage::price).reversed();case "Duration Short to Long"->Comparator.comparingInt(KumbhPackage::days);case "Duration Long to Short"->Comparator.comparingInt(KumbhPackage::days).reversed();case "Popularity"->Comparator.comparing(KumbhPackage::category);default->Comparator.comparingInt(KumbhPackage::price);}; return s.sorted(cmp).toList(); }
 private boolean match(Set<String> f,String value){return f.isEmpty()||f.stream().anyMatch(x->{String key=x.toLowerCase();String actual=value.toLowerCase();if(key.equals("private vehicle"))return actual.contains("private");if(key.equals("puja included"))return actual.contains("puja");if(key.equals("snan assistance"))return actual.contains("snan");if(key.equals("nashik sightseeing included"))return actual.contains("sightseeing");if(key.equals("senior citizen friendly"))return actual.contains("senior citizen");return actual.contains(key.replace("5d/4n","5d / 4n").replace("6d/5n+","6d"));});}
 private boolean matchBudget(Set<String> f,int p){return f.isEmpty()||f.stream().anyMatch(x->{if(x.contains("Under"))return p<5000;if(x.contains("50,000+"))return p>=50000;if(x.contains("5,000 – ₹10,000"))return p>=5000&&p<=10000;if(x.contains("10,000 – ₹20,000"))return p>=10000&&p<=20000;if(x.contains("20,000 – ₹35,000"))return p>=20000&&p<=35000;return p>=35000&&p<=50000;});}
}
