package licence_server.licence;

import common.Records;

import java.util.List;
import java.util.Map;

public interface ActiveLicenceListener {
    void update(Map<String, List<Records.LicenceData>> licences);
}
