package so.cb.adapter.acmt023.builder.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import so.cb.adapter.acmt023.builder.TemplateAcmt023DocumentBuilder;
import so.cb.adapter.acmt023.dto.AccountVerificationRequestJson;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class TemplateAcmt023DocumentBuilderImpl implements TemplateAcmt023DocumentBuilder {

    @Override
    public String buildDocumentXml(String instructingBic, AccountVerificationRequestJson request) {
        String timestampPattern = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC).format(Instant.now());
        String randomSuffix = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
        String msgId = instructingBic + timestampPattern + randomSuffix; // Exactly 31 characters
        String receiverBic = request.receiverBic();
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().atZone(ZoneOffset.UTC));

        log.info("Building Custom XML Template document:Document for acmt.023.001.03 (instructingBic: {}, receiverBic: {})",
                instructingBic, receiverBic);

        return """
                <document:Document xmlns:document="urn:iso:std:iso:20022:tech:xsd:acmt.023.001.03">
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
                """.formatted(
                msgId,
                timestamp,
                instructingBic,
                receiverBic,
                request.accountIdentifier(),
                request.identifierType().name()
        );
    }
}
