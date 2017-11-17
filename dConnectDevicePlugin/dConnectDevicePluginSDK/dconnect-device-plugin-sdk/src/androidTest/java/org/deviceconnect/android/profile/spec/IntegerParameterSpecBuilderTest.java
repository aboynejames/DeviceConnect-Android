package org.deviceconnect.android.profile.spec;


import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class IntegerParameterSpecBuilderTest implements DConnectSpecConstants {

    @Test
    public void testBuild_Enum() {
        IntegerParameterSpec.Builder builder = new IntegerParameterSpec.Builder();
        builder.setRequired(true);
        builder.setEnum(new Long[] {0L});
        IntegerParameterSpec dataSpec = builder.build();

        assertThat(dataSpec.getFormat(), is(equalTo(DataFormat.INT32)));
        assertThat(dataSpec.getEnum(), is(notNullValue()));
        assertThat(dataSpec.getMaximum(), is(nullValue()));
        assertThat(dataSpec.getMinimum(), is(nullValue()));
        assertThat(dataSpec.isExclusiveMaximum(), is(equalTo(false)));
        assertThat(dataSpec.isExclusiveMinimum(), is(equalTo(false)));
    }

    @Test
    public void testBuild_Length() {
        IntegerParameterSpec.Builder builder = new IntegerParameterSpec.Builder();
        builder.setRequired(true);
        builder.setMaximum(1L);
        builder.setMinimum(0L);
        builder.setExclusiveMaximum(true);
        builder.setExclusiveMinimum(true);
        IntegerParameterSpec dataSpec = builder.build();

        assertThat(dataSpec.getFormat(), is(equalTo(DataFormat.INT32)));
        assertThat(dataSpec.getEnum(), is(nullValue()));
        assertThat(dataSpec.getMaximum(), is(notNullValue()));
        assertThat(dataSpec.getMinimum(), is(notNullValue()));
        assertThat(dataSpec.isExclusiveMaximum(), is(equalTo(true)));
        assertThat(dataSpec.isExclusiveMinimum(), is(equalTo(true)));
    }

    @Test
    public void testBuild_Enum_Length() {
        IntegerParameterSpec.Builder builder = new IntegerParameterSpec.Builder();
        builder.setRequired(true);
        builder.setEnum(new Long[] {0L});
        builder.setMaximum(1L);
        builder.setMinimum(0L);
        builder.setExclusiveMaximum(true);
        builder.setExclusiveMinimum(true);
        IntegerParameterSpec dataSpec = builder.build();

        assertThat(dataSpec.getFormat(), is(equalTo(DataFormat.INT32)));
        assertThat(dataSpec.getEnum(), is(notNullValue()));
        assertThat(dataSpec.getMaximum(), is(nullValue()));
        assertThat(dataSpec.getMinimum(), is(nullValue()));
        assertThat(dataSpec.isExclusiveMaximum(), is(equalTo(false)));
        assertThat(dataSpec.isExclusiveMinimum(), is(equalTo(false)));
    }
}
