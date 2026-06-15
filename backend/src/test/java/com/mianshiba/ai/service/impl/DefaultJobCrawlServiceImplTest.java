package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultJobCrawlServiceImplTest {

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestHeadersUriSpec requestSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private DefaultJobCrawlServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DefaultJobCrawlServiceImpl(restClient);
    }

    @Test
    void crawl_shouldReturnCrawlResultWhenHttpSucceeds() {
        String url = "https://example.com/jobs/1";
        String html = "<html><head><title>Java \u540e\u7aef\u5f00\u53d1</title></head><body>\u804c\u4f4d\u63cf\u8ff0</body></html>";

        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(url)).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(html);

        var result = service.crawl(url);

        assertThat(result).isNotNull();
        assertThat(result.url()).isEqualTo(url);
        assertThat(result.title()).isEqualTo("Java \u540e\u7aef\u5f00\u53d1");
        assertThat(result.content()).contains("\u804c\u4f4d\u63cf\u8ff0");
        assertThat(result.sourcePlatform()).isEqualTo("unknown");
    }

    @Test
    void crawl_shouldThrowBusinessExceptionWhenHttpFails() {
        String url = "https://example.com/jobs/1";

        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(url)).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> service.crawl(url))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.JOB_CRAWL_ERROR.getCode());
    }

    @Test
    void crawl_shouldThrowBusinessExceptionWhenHtmlIsNull() {
        String url = "https://example.com/jobs/1";

        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(url)).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(null);

        assertThatThrownBy(() -> service.crawl(url))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.JOB_CRAWL_ERROR.getCode());
    }

    @Test
    void crawl_shouldDetectBossPlatform() {
        String url = "https://www.zhipin.com/job/123";
        String html = "<html><head><title>Boss \u5c97\u4f4d</title></head><body>desc</body></html>";

        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(url)).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(html);

        var result = service.crawl(url);

        assertThat(result.sourcePlatform()).isEqualTo("boss");
    }

    @Test
    void crawl_shouldDetectLagouPlatform() {
        String url = "https://www.lagou.com/jobs/123.html";

        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(url)).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("<html><head><title>test</title></head><body>desc</body></html>");

        var result = service.crawl(url);

        assertThat(result.sourcePlatform()).isEqualTo("lagou");
    }

    @Test
    void crawl_shouldExtractTextWithoutHtmlTags() {
        String url = "https://example.com/jobs/1";
        String html = """
                <html>
                <head><title>Test</title></head>
                <body>
                <h1>Job Title</h1>
                <p>Description here</p>
                <script>alert('x')</script>
                <style>.cls{color:red}</style>
                </body>
                </html>
                """;

        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(url)).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(html);

        var result = service.crawl(url);

        assertThat(result.content())
                .contains("Job Title")
                .contains("Description here")
                .doesNotContain("alert")
                .doesNotContain(".cls");
    }
}
