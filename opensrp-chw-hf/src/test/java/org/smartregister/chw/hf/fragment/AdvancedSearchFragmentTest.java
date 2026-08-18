package org.smartregister.chw.hf.fragment;

import org.junit.Test;

import java.util.Collections;

public class AdvancedSearchFragmentTest {

    @Test
    public void showResultsDoesNothingWhenFragmentViewIsUnavailable() {
        AdvancedSearchFragment fragment = new AdvancedSearchFragment();

        fragment.showResults(Collections.emptyList(), true);
    }
}
