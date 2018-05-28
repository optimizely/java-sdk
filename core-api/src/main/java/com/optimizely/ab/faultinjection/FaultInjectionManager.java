package com.optimizely.ab.faultinjection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manages Fault injection.
 * Throws exceptions on the acivated spot if isFaultInjectionEnabled is true.
 *
 * Usage:
 * 1) To take pre-treatment results, set treatException = false.
 * 2) To take post-treatment results, set treatException = true,
 * 3) To activate a spot use setActiveExceptionSpot. By default, no exception spot is active.
 *
 */
public class FaultInjectionManager {

    private static FaultInjectionManager instance;

    private static final Boolean isFaultInjectionEnabled = true;

    public static final String INJECTED_EXCEPTION = "InjectedException";

    private ExceptionSpot activeExceptionSpot = ExceptionSpot.Optimizely_activate3_spot1;

    private static final boolean treatException = false;

    /**
     * Private constructor to implement singleton
     */
    private FaultInjectionManager() {}

    public void setActiveExceptionSpot(ExceptionSpot spot) {
        this.activeExceptionSpot = spot;
    }

    public static FaultInjectionManager getInstance() {
        if( instance == null ) {
            instance = new FaultInjectionManager();
        }
        return instance;
    }

    /**
     * returns a list of off the exception spots from enum
     * @return
     */
    public List<ExceptionSpot> getAllExceptionSpots() {
        return new ArrayList<ExceptionSpot>(Arrays.asList(ExceptionSpot.values()));
    }

    /**
     * Injects fault at the given {@link ExceptionSpot} only that spot is marked at active and fault injection is enabled
     * @param spot
     */
    public void injectFault(ExceptionSpot spot) {
        if(isFaultInjectionEnabled && activeExceptionSpot == spot ) {
            throw new RuntimeException(INJECTED_EXCEPTION);
        }
    }


    /**
     * This simulates the case if there was no treatment applied.
     * this should be added in the catch blocks so that it throws exception again simulating the behavior when no treatment was applied.
     * the purpose is just to collect pre-treatment results
     * This only throws exception if treatException is false.
     */
    public void throwExceptionIfTreatmentDisabled() {
        if(isFaultInjectionEnabled && !treatException) {
            throw new RuntimeException(INJECTED_EXCEPTION);
        }
    }
}
