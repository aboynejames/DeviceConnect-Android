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
public class NumberParameterSpecBuilderTest implements DConnectSpecConstants {

    @Test
    public void testBuild_Enum() {
        NumberParameterSpec.Builder builder = new NumberParameterSpec.Builder();
        builder.setRequired(true);
        builder.setEnum(new Double[] {0.5d});
        NumberParameterSpec dataSpec = builder.build();

        assertThat(dataSpec.getFormat(), is(equalTo(DataFormat.FLOAT)));
        assertThat(dataSpec.getEnum(), is(notNullValue()));
        assertThat(dataSpec.getMaximum(), is(nullValue()));
        assertThat(dataSpec.getMinimum(), is(nullValue()));
        assertThat(dataSpec.isExclusiveMaximum(), is(equalTo(false)));
        assertThat(dataSpec.isExclusiveMinimum(), is(equalTo(false)));
    }

    @Test
    public void testBuild_Length() {
        NumberParameterSpec.Builder builder = new NumberParameterSpec.Builder();
        builder.setRequired(true);
        builder.setMaximum(1.5d);
        builder.setMinimum(0.5d);
        builder.setExclusiveMaximum(true);
        builder.setExclusiveMinimum(true);
        NumberParameterSpec dataSpec = builder.build();

        assertThat(dataSpec.getFormat(), is(equalTo(DataFormat.FLOAT)));
        assertThat(dataSpec.getEnum(), is(nullValue()));
        assertThat(dataSpec.getMaximum(), is(notNullValue()));
        assertThat(dataSpec.getMinimum(), is(notNullValue()));
        assertThat(dataSpec.isExclusiveMaximum(), is(equalTo(true)));
        assertThat(dataSpec.isExclusiveMinimum(), is(equalTo(true)));
    }

    @Test
    public void testBuild_Enum_Length() {
        NumberParameterSpec.Builder builder = new NumberParameterSpec.Builder();
        builder.setRequired(true);
        builder.setEnum(new Double[] {0.5d});
        builder.setMaximum(1.5d);
        builder.setMinimum(0.5d);
        builder.setExclusiveMaximum(true);
        builder.setExclusiveMinimum(true);
        NumberParameterSpec dataSpec = builder.build();

        assertThat(dataSpec.getFormat(), is(equalTo(DataFormat.FLOAT)));
        assertThat(dataSpec.getEnum(), is(notNullValue()));
        assertThat(dataSpec.getMaximum(), is(nullValue()));
        assertThat(dataSpec.getMinimum(), is(nullValue()));
        assertThat(dataSpec.isExclusiveMaximum(), is(equalTo(false)));
        assertThat(dataSpec.isExclusiveMinimum(), is(equalTo(false)));
    }
}
