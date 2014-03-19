package jenkins.metrics.impl.graphite;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import jenkins.metrics.api.Metrics;
import hudson.Plugin;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import org.apache.commons.lang.StringUtils;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Stephen Connolly
 */
public class PluginImpl extends Plugin {
    private static final Logger LOGGER = Logger.getLogger(PluginImpl.class.getName());

    private transient Map<GraphiteServer, GraphiteReporter> reporters;

    public PluginImpl() {
        this.reporters = new LinkedHashMap<GraphiteServer, GraphiteReporter>();
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public synchronized void stop() throws Exception {
        if (reporters != null) {
            for (GraphiteReporter r : reporters.values()) {
                r.stop();
            }
            reporters.clear();
        }
    }

    @Override
    public synchronized void postInitialize() throws Exception {
        updateReporters();
    }

    public synchronized void updateReporters() throws URISyntaxException {
        if (reporters == null) {
            reporters = new LinkedHashMap<GraphiteServer, GraphiteReporter>();
        }
        MetricRegistry registry = Metrics.metricRegistry();
        GraphiteServer.DescriptorImpl descriptor =
                Jenkins.getInstance().getDescriptorByType(GraphiteServer.DescriptorImpl.class);
        if (descriptor == null) {
            return;
        }
        String url = JenkinsLocationConfiguration.get().getUrl();
        URI uri = url == null ? null : new URI(url);
        String hostname = uri == null ? "localhost" : uri.getHost();
        Set<GraphiteServer> toStop = new HashSet<GraphiteServer>(reporters.keySet());
        for (GraphiteServer s : descriptor.getServers()) {
            toStop.remove(s);
            if (reporters.containsKey(s)) continue;
            Graphite g = new Graphite(new InetSocketAddress(s.getHostname(), s.getPort()));
            String prefix = StringUtils.isBlank(s.getPrefix()) ? hostname : s.getPrefix();
            GraphiteReporter r = GraphiteReporter.forRegistry(registry)
                    .prefixedWith(prefix)
                    .convertRatesTo(TimeUnit.MINUTES)
                    .convertDurationsTo(TimeUnit.SECONDS)
                    .filter(MetricFilter.ALL)
                    .build(g);
            reporters.put(s, r);
            LOGGER.log(Level.INFO, "Starting Graphite reporter to {0}:{1} with prefix {2}", new Object[]{
                    s.getHostname(), s.getPort(), prefix
            });
            r.start(1, TimeUnit.MINUTES);
        }
        for (GraphiteServer s: toStop) {
            GraphiteReporter r = reporters.get(s);
            reporters.remove(s);
            r.stop();
            LOGGER.log(Level.INFO, "Stopped Graphite reporter to {0}:{1} with prefix {2}", new Object[]{
                    s.getHostname(), s.getPort(), StringUtils.isBlank(s.getPrefix()) ? hostname : s.getPrefix()
            });
        }
    }
}
