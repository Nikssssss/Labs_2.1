import java.util.Objects;

public class DnsResolve {
    private String domain;
    private BasicAttachment basicAttachment;

    public DnsResolve(String domain, BasicAttachment basicAttachment) {
        this.domain = domain;
        this.basicAttachment = basicAttachment;
    }

    public String getDomain() {
        return domain;
    }

    public BasicAttachment getBasicAttachment() {
        return basicAttachment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DnsResolve)) return false;
        DnsResolve that = (DnsResolve) o;
        return getDomain().equals(that.getDomain()) &&
                getBasicAttachment().equals(that.getBasicAttachment());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDomain(), getBasicAttachment());
    }
}
