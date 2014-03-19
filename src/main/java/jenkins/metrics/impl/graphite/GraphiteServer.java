package jenkins.metrics.impl.graphite;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.concurrent.GuardedBy;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Stephen Connolly
 */
public class GraphiteServer extends AbstractDescribableImpl<GraphiteServer> {

    private static final Logger LOGGER = Logger.getLogger(GraphiteServer.class.getName());
    private final String hostname;

    private final int port;

    private final String prefix;

    @DataBoundConstructor
    public GraphiteServer(String hostname, int port, String prefix) {
        this.hostname = hostname;
        this.port = port;
        this.prefix = prefix;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GraphiteServer{");
        sb.append("hostname='").append(hostname).append('\'');
        sb.append(", port=").append(port);
        sb.append(", prefix='").append(prefix).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GraphiteServer that = (GraphiteServer) o;

        if (port != that.port) {
            return false;
        }
        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) {
            return false;
        }
        if (prefix != null ? !prefix.equals(that.prefix) : that.prefix != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = hostname != null ? hostname.hashCode() : 0;
        result = 31 * result + port;
        result = 31 * result + (prefix != null ? prefix.hashCode() : 0);
        return result;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GraphiteServer> {
        @GuardedBy("this")
        private List<GraphiteServer> servers;

        @Override
        public String getDisplayName() {
            return Messages.GraphiteServer_displayName();
        }

        public DescriptorImpl() {
            super();
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            setServers(req.bindJSONToList(GraphiteServer.class, json.get("servers")));
            return true;
        }

        public synchronized List<GraphiteServer> getServers() {
            return servers == null
                    ? Collections.<GraphiteServer>emptyList()
                    : Collections.unmodifiableList(new ArrayList<GraphiteServer>(servers));
        }

        public synchronized void setServers(List<GraphiteServer> servers) {
            this.servers = servers;
            save();
            try {
                Jenkins.getInstance().getPlugin(PluginImpl.class).updateReporters();
            } catch (URISyntaxException e) {
                LOGGER.log(Level.WARNING, "Could not update Graphite reporters", e);
            }
        }
    }

}
