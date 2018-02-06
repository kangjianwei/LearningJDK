package test.kang.breakiterator;

import java.text.BreakIterator;
import java.util.Locale;

// 测试分词器对字符(Unicode符号)的分割
public class BreakIteratorTest01 {
    // 𪚥是一个四字节符号
    static String txt1 = "中国、中国、中国；中国：“中国”。中国“中国”中国！中国？中国（中国）中国【中国】中国{中国}中国——中国《中国》中国……中国*中国~中国-中国（。）中国（！）中国（？）中国（（）中国（））中国（、）中国（；）中国 中国 \uD869\uDEA5\u9f98\u9f96\u9f8d";
    static String txt2 = "ChinaChina.ChinaChina. China?China!China/China,China;China:\"China\"China\"China\"China<China>China(China)China[China]China{China}China-China.China&China^China%China+China=China(.)China(?)China(!)China~China";
    
    static BreakIterator boundary;
    
    // 分词标准由底层算法决定，分词策略有些迷...
    public static void main(String[] args) {
        System.out.println("\n--------------------中文字符分割--------------------");
        boundary = BreakIterator.getCharacterInstance(Locale.CHINA);
        printCharacter(boundary, txt1);      // 切割中文符号，一个空格也算一个符号，可以识别四字节符号，如最后那个𪚥字。其他规则参考输出。
        
        System.out.println("\n--------------------英文字符分割--------------------");
        boundary = BreakIterator.getCharacterInstance(Locale.US);
        printCharacter(boundary, txt2);      // 切割英文字符。规则参考输出。
    }
    
    // 【解析“字符/符号”】
    public static void printCharacter(BreakIterator boundary, String source) {
        // 为分词器关联文本
        boundary.setText(source);
        
        int start = boundary.first();   // 第一个“字符/符号”的左边界
        int end = boundary.next();      // 第二个“字符/符号”的左边界
        
        // 初始时，不需要判断start的原因是此处的字符串受StringCharacterIterator约束，即不能为空
        while(end != BreakIterator.DONE) {
            System.out.print(String.format("打印范围[%3d,%3d)：", start, end));
            System.out.println(source.substring(start, end));
            
            start = end;                // 更新start
            end = boundary.next();      // 继续寻找下一个字符的左边界
        }
    }
}
