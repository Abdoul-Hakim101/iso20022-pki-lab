package so.cb.adapter.acmt023.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import so.cb.adapter.acmt023.builder.ProwideAcmt023DocumentBuilder;
import so.cb.adapter.acmt023.dto.AccountVerificationRequestJson;
import so.cb.adapter.acmt023.service.AccountVerificationService;
import so.cb.adapter.shared.config.AdapterProperties;
import so.cb.adapter.shared.service.FpEnvelopeBuilderService;
import so.cb.adapter.shared.service.XmlSignatureService;

@Slf4j
@Service("prowideAccountVerificationService")
@RequiredArgsConstructor
public class ProwideAccountVerificationServiceImpl implements AccountVerificationService {

    private final AdapterProperties adapterProperties;
    private final ProwideAcmt023DocumentBuilder prowideAcmt023DocumentBuilder;
    private final FpEnvelopeBuilderService fpEnvelopeBuilderService;
    private final XmlSignatureService xmlSignatureService;

    @Override
    public String createAndSignAcmt023Xml(AccountVerificationRequestJson request) {
        String instructingBic = adapterProperties.getBankBic();
        String receiverBic = request.receiverBic();

        log.info("Processing Prowide Strategy acmt.023.001.03 Pipeline (instructingBic: {}, receiverBic: {}, account: {})",
                instructingBic, receiverBic, request.accountIdentifier());

        // Step 1: Prowide Builder generates inner ISO 20022 <document:Document> XML
        String innerDocumentXml = prowideAcmt023DocumentBuilder.buildDocumentXml(instructingBic, request);

        // Step 2: Shared Envelope Service wraps inner document inside <FPEnvelope> & <header:AppHdr>
        String unsignedEnvelopeXml = fpEnvelopeBuilderService.wrapInFpEnvelope(
                innerDocumentXml, instructingBic, receiverBic, "acmt.023.001.03"
        );

        // Step 3: Shared Security Service performs W3C XML Digital Signing in <document:Sgntr>
        String signedXml = xmlSignatureService.signXml(unsignedEnvelopeXml);

        log.info("Successfully completed Prowide strategy pipeline for acmt.023.001.03 request.");
        return signedXml;
    }
}
