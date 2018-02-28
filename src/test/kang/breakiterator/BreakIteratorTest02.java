package test.kang.breakiterator;

import java.text.BreakIterator;
import java.util.Locale;

// 测试分词器对单词的分割
public class BreakIteratorTest02 {
    // 𪚥是一个四字节符号
    static String txt1 = "中国、中国、中国；中国：“中国”。中国“中国”中国！中国？中国（中国）中国【中国】中国{中国}中国——中国《中国》中国……中国*中国~中国-中国（。）中国（！）中国（？）中国（（）中国（））中国（、）中国（；）中国 中国 \uD869\uDEA5\u9f98\u9f96\u9f8d";
    static String txt2 = "ChinaChina.ChinaChina. China?China!China/China,China;China:\"China\"China\"China\"China<China>China(China)China[China]China{China}China-China.China&China^China%China+China=China(.)China(?)China(!)China~China";
    
    static BreakIterator boundary;
    
    // 分词标准由底层算法决定，分词策略有些迷...
    public static void main(String[] args) {
        System.out.println("\n--------------------中文单词分割--------------------");
        boundary = BreakIterator.getWordInstance(Locale.CHINA);
        printWord(boundary, txt1);      // 切割中文单词，标点、空格被单独切割，以标点作为单词与单词的间隔。其他规则参考输出。
    
        System.out.println("\n--------------------英文单词分割--------------------");
        boundary = BreakIterator.getWordInstance(Locale.US);
        printWord(boundary, txt2);      // 切割英文单词，标点、空格被单独切割，以标点作为单词与单词的间隔。独立的英文点号和连字符不作为分割标记。其他规则参考输出。
    }
    
    // 【解析“单词”】
    public static void printWord(BreakIterator boundary, String source) {
        // 为分词器关联文本
        boundary.setText(source);
        
        int start = boundary.first();     // 第一个“单词”的左边界
        int end   = boundary.next();      // 第二个“单词”的左边界
        
        // 初始时，不需要判断start的原因是此处的字符串受StringCharacterIterator约束，即不能为空
        while(end != BreakIterator.DONE) {
            System.out.print(String.format("打印范围[%3d,%3d)：", start, end));
            
            System.out.println(source.substring(start, end));
            
            start = end;                // 更新start
            end = boundary.next();      // 继续寻找下一个单词的左边界
        }
    }
}
