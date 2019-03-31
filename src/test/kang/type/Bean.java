package test.kang.type;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Bean<X, Y extends Number, Z extends Date & CharSequence & Set> {
    List<String> lista = null;
    List<?> listb = null;
    Map.Entry<Integer, Thread> entry = null;
    
    List<? extends Map> listc = null;   // 通配符上界
    List<? super HashMap> listd = null; // 通配符下界
    
    X[] xa;
    X[][] xb;
}
