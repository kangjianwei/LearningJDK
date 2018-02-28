package test.kang.breakiterator;

import java.text.BreakIterator;
import java.util.Locale;

// 测试分词器对行的分割，即确定哪些边界后面可以跟换行符（单词间不换行，标点一般会被分配在上一行）
public class BreakIteratorTest03 {
    // 𪚥是一个四字节符号
    static String txt1 = "中国、中国、中国；中国：“中国”。中国“中国”中国！中国？中国（中国）中国【中国】中国{中国}中国——中国《中国》中国……中国*中国~中国-中国（。）中国（！）中国（？）中国（（）中国（））中国（、）中国（；）中国 中国 \uD869\uDEA5\u9f98\u9f96\u9f8d";
    static String txt2 = "ChinaChina.ChinaChina. China?China!China/China,China;China:\"China\"China\"China\"China<China>China(China)China[China]China{China}China-China.China&China^China%China+China=China(.)China(?)China(!)China~China";
    
    static BreakIterator boundary;
    
    // 分词标准由底层算法决定，分词策略有些迷...
    public static void main(String[] args) {
        System.out.println("\n--------------------中文行分割--------------------");
        boundary = BreakIterator.getLineInstance(Locale.CHINA);
        printLine(boundary, txt1);      // 切割中文行。规则参考输出结果
    
        System.out.println("\n--------------------英文行分割--------------------");
        boundary = BreakIterator.getLineInstance(Locale.US);
        printLine(boundary, txt2);      // 切割英文行。规则参考输出结果
    }
    
    // 【解析“行”】注意，空白符会被切分在上一个“行”中
    public static void printLine(BreakIterator boundary, String source) {
        // 为分词器关联文本
        boundary.setText(source);
        
        int start = boundary.first();     // 第一个“行”的左边界
        int end   = boundary.next();      // 第二个“行”的左边界
        
        // 初始时，不需要判断start的原因是此处的字符串受StringCharacterIterator约束，即不能为空
        while(end != BreakIterator.DONE) {
            System.out.print(String.format("打印范围[%3d,%3d)：", start, end));
            
            System.out.println(source.substring(start, end));
            
            start = end;                // 更新start
            end = boundary.next();      // 继续寻找下一个行的左边界
        }
    }
}
