// SPDX-FileCopyrightText: 2020 iteratec GmbH
//
// SPDX-License-Identifier: Apache-2.0
package io.securecodebox.persistence;

import io.securecodebox.persistence.config.PersistenceProviderConfig;
import io.securecodebox.persistence.defectdojo.config.DefectDojoConfig;
import io.securecodebox.persistence.defectdojo.models.ScanFile;
import io.securecodebox.persistence.defectdojo.service.EndpointService;
import io.securecodebox.persistence.mapping.DefectDojoFindingToSecureCodeBoxMapper;
import io.securecodebox.persistence.models.Scan;
import io.securecodebox.persistence.service.KubernetesService;
import io.securecodebox.persistence.service.S3Service;
import io.securecodebox.persistence.strategies.VersionedEngagementsStrategy;
import io.securecodebox.persistence.util.ScanNameMapping;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URL;
import java.util.stream.Collectors;

public class DefectDojoPersistenceProvider {
  private static final Logger LOG = LoggerFactory.getLogger(DefectDojoPersistenceProvider.class);

  public static void main(String[] args) throws Exception {
    LOG.info("Starting DefectDojo persistence provider");

    var persistenceProviderConfig = new PersistenceProviderConfig(args);

    var s3Service = new S3Service();
    var kubernetesService = new KubernetesService();
    kubernetesService.init();

    var scan = new Scan(kubernetesService.getScanFromKubernetes());
    scan.validate();

    var config = DefectDojoConfig.fromEnv();
    LOG.info("Downloading Scan Result");
    String downloadUrl;
    String scanType = scan.getSpec().getScanType();
    ScanNameMapping scanNameMapping = ScanNameMapping.bySecureCodeBoxScanType(scanType);
    if (scanNameMapping == ScanNameMapping.GENERIC) {
      LOG.debug("No explicit Parser specified for ScanType {}, using Findings JSON Scan Result", scanType);
      downloadUrl = persistenceProviderConfig.getFindingDownloadUrl();
    } else {
      LOG.debug("Explicit Parser is specified for ScanType {}, using Raw Scan Result", scanNameMapping.scanType);
      downloadUrl = persistenceProviderConfig.getRawResultDownloadUrl();
    }
    var scanResults = s3Service.downloadFile(downloadUrl);
    LOG.info("Finished Downloading Scan Result");
    LOG.debug("Scan Result: {}", scanResults);

    LOG.info("Uploading Findings to DefectDojo at: {}", config.getUrl());

    var defectdojoImportStrategy = new VersionedEngagementsStrategy();
    defectdojoImportStrategy.init(config);
    var scanResultFile = new ScanFile(scanResults, FilenameUtils.getName(new URL(downloadUrl).getPath()));
    var defectDojoFindings = defectdojoImportStrategy.run(scan, scanResultFile);

    LOG.info("Identified total Number of findings in DefectDojo: {}", defectDojoFindings.size());

    if (persistenceProviderConfig.isReadAndWrite()) {
      var endpointService = new EndpointService(config);
      var mapper = new DefectDojoFindingToSecureCodeBoxMapper(config, endpointService);

      LOG.info("Overwriting secureCodeBox findings with the findings from DefectDojo.");

      var findings = defectDojoFindings.stream()
        .map(mapper::fromDefectDojoFining)
        .collect(Collectors.toList());

      LOG.debug("Mapped Findings: {}", findings);

      s3Service.overwriteFindings(persistenceProviderConfig.getFindingUploadUrl(), findings);
      kubernetesService.updateScanInKubernetes(findings);
    }

    LOG.info("DefectDojo Persistence Completed");
  }
}
