package jenkins.metrics.impl.graphite;

import org.junit.Assert;
import org.junit.Test;


/**
 * Some trivial tests
 */
public class GraphiteTest {
    @Test
    public void testTimingGetter() throws Exception {
        String value = System.getProperty(PluginImpl.SAMPLING_INTERVAL_PROPERTY);
        if (value == null) {
            Assert.assertEquals(60, PluginImpl.getSamplingIntervalSeconds());
        }

        // For some reason I need to call setproperty twice for it to show up here?
        System.setProperty(PluginImpl.SAMPLING_INTERVAL_PROPERTY, "5");
        Assert.assertEquals(5, PluginImpl.getSamplingIntervalSeconds());

        System.clearProperty(PluginImpl.SAMPLING_INTERVAL_PROPERTY);
        Assert.assertEquals(60, PluginImpl.getSamplingIntervalSeconds());

        System.setProperty(PluginImpl.SAMPLING_INTERVAL_PROPERTY, "false");
        Assert.assertEquals(60, PluginImpl.getSamplingIntervalSeconds());
    }
}
