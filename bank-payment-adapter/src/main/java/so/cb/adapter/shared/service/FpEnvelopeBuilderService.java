package so.cb.adapter.shared.service;

public interface FpEnvelopeBuilderService {
    String wrapInFpEnvelope(String innerDocumentXml, String instructingBic, String receiverBic, String messageDefinition);
}
