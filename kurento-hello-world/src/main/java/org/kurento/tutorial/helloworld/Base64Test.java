package org.kurento.tutorial.helloworld;

import org.apache.commons.codec.binary.Base64;

public class Base64Test {

    public static void main(String[] args){
        //String base64String = "0083888e309c4255bcc378597deb302c:87d4faec0e5a4fba99432edd6e4036e7";
        String base64String="bd7324ee54294f68bad88762dc383c66:112879d6153743f092724d591ae1dc52";
        String result = Base64.encodeBase64String(base64String.getBytes());
        System.err.println(result);

    }
}
