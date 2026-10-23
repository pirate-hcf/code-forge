package com.codeforge;

/**
 * Hello world!
 *
 */
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
