package so.cb.pki.csr.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import so.cb.pki.csr.service.CsrService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CsrController.class)
class CsrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CsrService csrService;

    @Test
    void uploadCsr_Multipart_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csr",
                "text/plain",
                "-----BEGIN CERTIFICATE REQUEST-----\ntest\n-----END CERTIFICATE REQUEST-----".getBytes()
        );

        doNothing().when(csrService).uploadCsr(any(), any());

        mockMvc.perform(multipart("/api/v1/csrs")
                        .file(file)
                        .param("bic", "CBKSSOM1XXX"))
                .andExpect(status().isCreated());

        verify(csrService).uploadCsr(any(), any());
    }
}
