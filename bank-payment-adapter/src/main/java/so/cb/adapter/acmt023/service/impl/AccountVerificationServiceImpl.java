package so.cb.adapter.acmt023.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import so.cb.adapter.acmt023.dto.AccountVerificationRequestJson;
import so.cb.adapter.acmt023.service.AccountVerificationService;
import so.cb.adapter.shared.config.AdapterProperties;
import so.cb.adapter.shared.security.XmlSignatureSigningService;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountVerificationServiceImpl implements AccountVerificationService {

    private final AdapterProperties adapterProperties;
    private final XmlSignatureSigningService xmlSignatureSigningService;

    @Override
    public String createAndSignAcmt023Xml(AccountVerificationRequestJson request) {
        String instructingBic = adapterProperties.getBankBic();
        String receiverBic = request.receiverBic();
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atZone(ZoneOffset.UTC));
        String bizMsgIdr = instructingBic + System.currentTimeMillis();
        String msgId = instructingBic + (System.currentTimeMillis() + 1);

        log.info("Creating ISO 20022 FPEnvelope acmt.023.001.03 for instructing BIC: {}, receiver BIC: {}, accountIdentifier: {}, type: {}",
                instructingBic, receiverBic, request.accountIdentifier(), request.identifierType());

        String rawEnvelopeXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FPEnvelope xmlns:header="urn:iso:std:iso:20022:tech:xsd:head.001.001.03"
                            xmlns:document="urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03"
                            xmlns="urn:iso:std:iso:20022:tech:xsd:verification_request">
                  <header:AppHdr>
                    <header:Fr>
                      <header:FIId>
                        <header:FinInstnId>
                          <header:Othr>
                            <header:Id>%s</header:Id>
                          </header:Othr>
                        </header:FinInstnId>
                      </header:FIId>
                    </header:Fr>
                    <header:To>
                      <header:FIId>
                        <header:FinInstnId>
                          <header:Othr>
                            <header:Id>%s</header:Id>
                          </header:Othr>
                        </header:FinInstnId>
                      </header:FIId>
                    </header:To>
                    <header:BizMsgIdr>%s</header:BizMsgIdr>
                    <header:MsgDefIdr>acmt.023.001.03</header:MsgDefIdr>
                    <header:CreDt>%s</header:CreDt>
                    <document:Sgntr xmlns:document="urn:iso:std:iso:20022:tech:xsd:head.001.001.03"/>
                  </header:AppHdr>
                  <document:Document>
                    <document:IdVrfctnReq>
                      <document:Assgnmt>
                        <document:MsgId>%s</document:MsgId>
                        <document:CreDtTm>%s</document:CreDtTm>
                        <document:Assgnr>
                          <document:Agt>
                            <document:FinInstnId>
                              <document:Othr>
                                <document:Id>%s</document:Id>
                              </document:Othr>
                            </document:FinInstnId>
                          </document:Agt>
                        </document:Assgnr>
                        <document:Assgne>
                          <document:Agt>
                            <document:FinInstnId>
                              <document:Othr>
                                <document:Id>%s</document:Id>
                              </document:Othr>
                            </document:FinInstnId>
                          </document:Agt>
                        </document:Assgne>
                      </document:Assgnmt>
                      <document:Vrfctn>
                        <document:Id>FP</document:Id>
                        <document:PtyAndAcctId>
                          <document:Acct>
                            <document:Id>
                              <document:Othr>
                                <document:Id>%s</document:Id>
                                <document:SchmeNm>
                                  <document:Prtry>%s</document:Prtry>
                                </document:SchmeNm>
                              </document:Othr>
                            </document:Id>
                          </document:Acct>
                        </document:PtyAndAcctId>
                      </document:Vrfctn>
                    </document:IdVrfctnReq>
                  </document:Document>
                </FPEnvelope>
                """.formatted(
                instructingBic,
                receiverBic,
                bizMsgIdr,
                timestamp,
                msgId,
                timestamp,
                instructingBic,
                receiverBic,
                request.accountIdentifier(),
                request.identifierType().name()
        );

        String signedXml = xmlSignatureSigningService.signXml(rawEnvelopeXml);
        log.info("Successfully generated and signed FPEnvelope ISO 20022 acmt.023.001.03 messageId: {}", msgId);
        return signedXml;
    }
}
