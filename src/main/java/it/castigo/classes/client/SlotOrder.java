package it.castigo.classes.client;

import java.util.*;

public final class SlotOrder {
    private SlotOrder() {}
    public static boolean valid(List<String> order,List<String> available) {
        return order.size()==8&&new HashSet<>(order).size()==8&&new HashSet<>(order).equals(new HashSet<>(available));
    }
    public static List<String> swap(List<String> order,int a,int b) {
        if(order.size()!=8||a<0||a>7||b<0||b>7)throw new IllegalArgumentException("Invalid skill slots");
        List<String> result=new ArrayList<>(order);Collections.swap(result,a,b);return result;
    }
}
