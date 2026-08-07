package so.cb.adapter.acmt023.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import so.cb.adapter.acmt023.builder.JaxbAcmt023DocumentBuilder;
import so.cb.adapter.acmt023.dto.AccountVerificationRequestJson;
import so.cb.adapter.acmt023.service.AccountVerificationService;
import so.cb.adapter.shared.config.AdapterProperties;
import so.cb.adapter.shared.service.FpEnvelopeBuilderService;
import so.cb.adapter.shared.service.XmlSignatureService;

@Slf4j
@Service("jaxbAccountVerificationService")
@RequiredArgsConstructor
public class JaxbAccountVerificationServiceImpl implements AccountVerificationService {

    private final AdapterProperties adapterProperties;
    private final JaxbAcmt023DocumentBuilder jaxbDocumentBuilder;
    private final FpEnvelopeBuilderService fpEnvelopeBuilderService;
    private final XmlSignatureService xmlSignatureService;

    @Override
    public String createAndSignAcmt023Xml(AccountVerificationRequestJson request) {
        String instructingBic = adapterProperties.getBankBic();
        String receiverBic = request.receiverBic();

        log.info("Processing JAXB Strategy acmt.023.001.03 Pipeline (instructingBic: {}, receiverBic: {}, account: {})",
                instructingBic, receiverBic, request.accountIdentifier());

        // 1. Build document XML using JAXB
        String innerDocumentXml = jaxbDocumentBuilder.buildDocumentXml(instructingBic, request);

        // 2. Wrap inside FPEnvelope & AppHdr
        String unsignedEnvelopeXml = fpEnvelopeBuilderService.wrapInFpEnvelope(innerDocumentXml, instructingBic, receiverBic, "acmt.023.001.03");

        // 3. Sign using XAdES-BES
        String signedEnvelopeXml = xmlSignatureService.signXml(unsignedEnvelopeXml);

        log.info("Successfully completed JAXB strategy pipeline for acmt.023.001.03 request.");
        return signedEnvelopeXml;
    }
}
