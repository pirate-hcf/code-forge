package com.codeforge;

import com.codeforge.util.CodeUtil;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Hello world!
 *
 */
@EnableAsync
@SpringBootApplication
public class CodeforgeApplication
{
    public static void main( String[] args )
    {
        CodeUtil codeUtil = new CodeUtil();
        String code1 = codeUtil.generateCode(109,1);
        System.out.println(code1);
        System.out.println(codeUtil.parseCode(code1));
    }
}
