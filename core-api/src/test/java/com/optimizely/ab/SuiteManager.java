package com.optimizely.ab;

import com.optimizely.ab.faultinjection.ExceptionSpot;

import java.io.*;

public class SuiteManager {
    public static void main(String[] args) {
        try {
            PrintWriter p = new PrintWriter(new FileOutputStream(new File("core-api/results.csv"), false));
            p.println(" % Pass, % Exception, % Assertion Failure, Fault Spot");
            p.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for(ExceptionSpot spot : ExceptionSpot.values()) {
            runTestSuiteFromShell(spot);
        }
    }

    private static void runTestSuiteFromShell(ExceptionSpot spot) {
        try {

            Process proc = Runtime.getRuntime().exec("./gradlew :core-api:test --rerun-tasks --tests com.optimizely.ab.TestSuiteRunner.main -Darg1=" + spot.name(),
                    null, new File("/home/zeeshan/mc/repos/java-sdk/"));

            proc.waitFor();

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            // read the output from the command
            System.out.println("Here is the standard output of the command:\n");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }

            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        } catch (IOException e) {
            System.out.println(e.getCause());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
