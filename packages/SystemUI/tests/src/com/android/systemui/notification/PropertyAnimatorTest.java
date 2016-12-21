/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.notification;

import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.support.test.annotation.UiThreadTest;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.systemui.Interpolators;
import com.android.systemui.R;

import android.util.FloatProperty;
import android.util.Property;
import android.view.View;
import android.view.animation.Interpolator;

import com.android.systemui.SysuiTestCase;
import com.android.systemui.statusbar.notification.PropertyAnimator;
import com.android.systemui.statusbar.stack.AnimationFilter;
import com.android.systemui.statusbar.stack.AnimationProperties;
import com.android.systemui.statusbar.stack.ViewState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class PropertyAnimatorTest extends SysuiTestCase {

    private View mView;
    private FloatProperty<View> mEffectiveProperty = new FloatProperty<View>("TEST") {
        public float mValue = 100;

        @Override
        public void setValue(View view, float value) {
            mValue = value;
        }

        @Override
        public Float get(View object) {
            return mValue;
        }
    };
    private PropertyAnimator.AnimatableProperty mProperty
            = new PropertyAnimator.AnimatableProperty() {

        @Override
        public int getAnimationStartTag() {
            return R.id.scale_x_animator_start_value_tag;
        }

        @Override
        public int getAnimationEndTag() {
            return R.id.scale_x_animator_end_value_tag;
        }

        @Override
        public int getAnimatorTag() {
            return R.id.scale_x_animator_tag;
        }

        @Override
        public Property getProperty() {
            return mEffectiveProperty;
        }
    };
    private AnimatorListenerAdapter mFinishListener = mock(AnimatorListenerAdapter.class);
    private AnimationProperties mAnimationProperties = new AnimationProperties() {
        @Override
        public AnimationFilter getAnimationFilter() {
            return mAnimationFilter;
        }

        @Override
        public AnimatorListenerAdapter getAnimationFinishListener() {
            return mFinishListener;
        }
    }.setDuration(200);
    private AnimationFilter mAnimationFilter = new AnimationFilter();
    private Interpolator mTestInterpolator = Interpolators.ALPHA_IN;


    @Before
    @UiThreadTest
    public void setUp() {
        mView = new View(getContext());
    }

    @Test
    @UiThreadTest
    public void testAnimationStarted() {
        mAnimationFilter.reset();
        mAnimationFilter.animate(mProperty.getProperty());
        PropertyAnimator.startAnimation(mView, mProperty, 200, mAnimationProperties);
        assertTrue(ViewState.isAnimating(mView, mProperty));
    }

    @Test
    @UiThreadTest
    public void testNoAnimationStarted() {
        mAnimationFilter.reset();
        PropertyAnimator.startAnimation(mView, mProperty, 200, mAnimationProperties);
        assertFalse(ViewState.isAnimating(mView, mProperty));
    }

    @Test
    @UiThreadTest
    public void testEndValueUpdated() {
        mAnimationFilter.reset();
        mAnimationFilter.animate(mProperty.getProperty());
        PropertyAnimator.startAnimation(mView, mProperty, 200f, mAnimationProperties);
        assertEquals(ViewState.getChildTag(mView, mProperty.getAnimationEndTag()),
                Float.valueOf(200f));
    }

    @Test
    @UiThreadTest
    public void testStartTagUpdated() {
        mEffectiveProperty.set(mView, 100f);
        mAnimationFilter.reset();
        mAnimationFilter.animate(mProperty.getProperty());
        PropertyAnimator.startAnimation(mView, mProperty, 200f, mAnimationProperties);
        assertEquals(ViewState.getChildTag(mView, mProperty.getAnimationStartTag()),
                Float.valueOf(100f));
    }

    @Test
    @UiThreadTest
    public void testValueIsSetUnAnimated() {
        mAnimationFilter.reset();
        PropertyAnimator.startAnimation(mView, mProperty, 200f, mAnimationProperties);
        assertEquals(Float.valueOf(200f), mEffectiveProperty.get(mView));
    }

    @Test
    @UiThreadTest
    public void testAnimationToRightValueUpdated() {
        mAnimationFilter.reset();
        mAnimationFilter.animate(mProperty.getProperty());
        PropertyAnimator.startAnimation(mView, mProperty, 200f, mAnimationProperties);
        mAnimationFilter.reset();
        PropertyAnimator.startAnimation(mView, mProperty, 220f, mAnimationProperties);
        assertTrue(ViewState.isAnimating(mView, mProperty));
        assertEquals(ViewState.getChildTag(mView, mProperty.getAnimationEndTag()),
                Float.valueOf(220f));
    }

    @Test
    @UiThreadTest
    public void testAnimationToRightValueUpdateAnimated() {
        mAnimationFilter.reset();
        mAnimationFilter.animate(mProperty.getProperty());
        PropertyAnimator.startAnimation(mView, mProperty, 200f, mAnimationProperties);
        mAnimationFilter.reset();
        mAnimationFilter.animate(mProperty.getProperty());
        PropertyAnimator.startAnimation(mView, mProperty, 220f, mAnimationProperties);
        assertTrue(ViewState.isAnimating(mView, mProperty));
        assertEquals(ViewState.getChildTag(mView, mProperty.getAnimationEndTag()),
                Float.valueOf(220f));
    }

    @Test
    @UiThreadTest
    public void testStartTagShiftedWhenChanging() {
        mEffectiveProperty.set(mView, 100f);
        mAnimationFilter.reset();
        mAnimationFilter.animate(mProperty.getProperty());
        PropertyAnimator.startAnimation(mView, mProperty, 200f, mAnimationProperties);
        mAnimationFilter.reset();
        PropertyAnimator.startAnimation(mView, mProperty, 220f, mAnimationProperties);
        assertEquals(ViewState.getChildTag(mView, mProperty.getAnimationStartTag()),
                Float.valueOf(120f));
    }

    @Test
    @UiThreadTest
    public void testUsingDuration() {
        mAnimationFilter.reset();
        mAnimationFilter.animate(mProperty.getProperty());
        mAnimationProperties.setDuration(500);
        PropertyAnimator.startAnimation(mView, mProperty, 200f, mAnimationProperties);
        ValueAnimator animator = ViewState.getChildTag(mView, mProperty.getAnimatorTag());
        assertNotNull(animator);
        assertEquals(animator.getDuration(), 500);
    }

    @Test
    @UiThreadTest
    public void testUsingDelay() {
        mAnimationFilter.reset();
        mAnimationFilter.animate(mProperty.getProperty());
        mAnimationProperties.setDelay(200);
        PropertyAnimator.startAnimation(mView, mProperty, 200f, mAnimationProperties);
        ValueAnimator animator = ViewState.getChildTag(mView, mProperty.getAnimatorTag());
        assertNotNull(animator);
        assertEquals(animator.getStartDelay(), 200);
    }

    @Test
    @UiThreadTest
    public void testUsingInterpolator() {
        mAnimationFilter.reset();
        mAnimationFilter.animate(mProperty.getProperty());
        mAnimationProperties.setCustomInterpolator(mEffectiveProperty, mTestInterpolator);
        PropertyAnimator.startAnimation(mView, mProperty, 200f, mAnimationProperties);
        ValueAnimator animator = ViewState.getChildTag(mView, mProperty.getAnimatorTag());
        assertNotNull(animator);
        assertEquals(animator.getInterpolator(), mTestInterpolator);
    }

    @Test
    @UiThreadTest
    public void testUsingListener() {
        mAnimationFilter.reset();
        mAnimationFilter.animate(mProperty.getProperty());
        mAnimationProperties.setCustomInterpolator(mEffectiveProperty, mTestInterpolator);
        PropertyAnimator.startAnimation(mView, mProperty, 200f, mAnimationProperties);
        ValueAnimator animator = ViewState.getChildTag(mView, mProperty.getAnimatorTag());
        assertNotNull(animator);
        assertTrue(animator.getListeners().contains(mFinishListener));
    }
}
