package com.optimizely.ab;

import com.optimizely.ab.bucketing.BucketerTest;
import com.optimizely.ab.bucketing.DecisionServiceTest;
import com.optimizely.ab.bucketing.internal.MurmurHash3Test;
import com.optimizely.ab.config.ProjectConfigTest;
import com.optimizely.ab.config.VariationTest;
import com.optimizely.ab.config.audience.AudienceConditionEvaluationTest;
import com.optimizely.ab.config.parser.*;
import com.optimizely.ab.event.NoopEventHandlerTest;
import com.optimizely.ab.event.internal.EventBuilderTest;
import com.optimizely.ab.event.internal.serializer.*;
import com.optimizely.ab.faultinjection.ExceptionSpot;
import com.optimizely.ab.faultinjection.FaultInjectionManager;
import com.optimizely.ab.internal.ExperimentUtilsTest;
import com.optimizely.ab.notification.NotificationCenterTest;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TestSuiteRunner {

    @Test
    public void main() {
        String spotString = System.getProperty("arg1");
        TestSuiteRunner runner = new TestSuiteRunner();
        try {
            runner.runAllTests(ExceptionSpot.valueOf(spotString));
        } catch (IllegalArgumentException e) {
            // eat it
        }
    }

    /**
     * Loops through all the exception spots and runs the complete test suite once for each spot
     * and collects results in results.csv
     */
    public void runAllTests(ExceptionSpot spot) {


        FaultInjectionManager mgr = FaultInjectionManager.getInstance();

        List<String> csvOutput = new ArrayList<String>();

        mgr.setActiveExceptionSpot(spot);

        // collect results of test suite run
        Result result = this.runTestSuite();

        // separating failures due to exceptions and assertion failures
        int exceptionCount = 0;
        int failedAssertionCount = 0;
        for (Failure f : result.getFailures()) {
            // if the exception type is INJECTED_EXCEPTION, it means it was failure due to exception
            if(f.getMessage() != null && f.getMessage().equals(FaultInjectionManager.INJECTED_EXCEPTION)) {
                exceptionCount++;
            } else {
                failedAssertionCount++;
            }
        }

        Integer passCount = result.getRunCount() - result.getFailureCount();

        csvOutput.add(
                getPercentage(passCount, result.getRunCount()) + "," +
                        getPercentage(exceptionCount, result.getRunCount()) + "," +
                        getPercentage(failedAssertionCount, result.getRunCount()) + "," +
                        spot.getReadableName());


        writeCSVToFile(csvOutput);

    }

    private Result runTestSuite() {
        return new JUnitCore().run (
                MurmurHash3Test.class,
                BucketerTest.class,
                DecisionServiceTest.class,

                AudienceConditionEvaluationTest.class,
                DefaultConfigParserTest.class,
                GsonConfigParserTest.class,
                JacksonConfigParserTest.class,
                JsonConfigParserTest.class,
                JsonSimpleConfigParserTest.class,
                ProjectConfigTest.class,
                VariationTest.class,

                GsonSerializerTest.class,
                JacksonSerializerTest.class,
                JsonSerializerTest.class,
                JsonSimpleSerializerTest.class,

                NoopEventHandlerTest.class,

                ExperimentUtilsTest.class,
                NotificationCenterTest.class,

                OptimizelyBuilderTest.class,
                EventBuilderTest.class,
                OptimizelyTest.class

        );
    }

    private String getPercentage(Integer part, Integer total) {
        return new DecimalFormat("####0.00").format(( part / (double) total ) * 100);
    }

    private void writeCSVToFile(List<String> csv) {
        try {
            PrintWriter pr = new PrintWriter(new FileOutputStream(new File("results.csv"), true));
            for( String csvLine : csv) {
                pr.println(csvLine);
            }
            pr.close();
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }
}