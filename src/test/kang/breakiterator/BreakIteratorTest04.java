package test.kang.breakiterator;

import java.text.BreakIterator;
import java.util.Locale;

// 测试分词器对句子的分割
public class BreakIteratorTest04 {
    // 𪚥是一个四字节符号
    static String txt1 = "中国、中国、中国；中国：“中国”。中国“中国”中国！中国？中国（中国）中国【中国】中国{中国}中国——中国《中国》中国……中国*中国~中国-中国（。）中国（！）中国（？）中国（（）中国（））中国（、）中国（；）中国 中国 \uD869\uDEA5\u9f98\u9f96\u9f8d";
    static String txt2 = "ChinaChina.ChinaChina. China?China!China/China,China;China:\"China\"China\"China\"China<China>China(China)China[China]China{China}China-China.China&China^China%China+China=China(.)China(?)China(!)China~China";
    
    static BreakIterator boundary;
    
    // 分词标准由底层算法决定，分词策略有些迷...
    public static void main(String[] args) {
        System.out.println("\n--------------------中文句子分割--------------------");
        boundary = BreakIterator.getSentenceInstance(Locale.CHINA);
        printSentence(boundary, txt1);      // 切割中文句子，遇到问号，感叹号，句号则分割。其他规则参考输出。
    
        System.out.println("\n--------------------英文句子分割--------------------");
        boundary = BreakIterator.getSentenceInstance(Locale.US);
        printSentence(boundary, txt2);      // 切割英文句子，遇到空格，问号，感叹号则分割，遇到英文点号时不会分割。其他规则参考输出。
    }
    
    // 【解析“句子”】注意，空白符会被切分在上一个“句子”中
    public static void printSentence(BreakIterator boundary, String source) {
        // 为分词器关联文本
        boundary.setText(source);
        
        int start = boundary.first();     // 第一个“句子”的左边界
        int end   = boundary.next();      // 第二个“句子”的左边界
        
        // 初始时，不需要判断start的原因是此处的字符串受StringCharacterIterator约束，即不能为空
        while(end != BreakIterator.DONE) {
            System.out.print(String.format("打印范围[%3d,%3d)：", start, end));
            
            System.out.println(source.substring(start, end));
            
            start = end;                // 更新start
            end = boundary.next();      // 继续寻找下一个句子的左边界
        }
    }
}
