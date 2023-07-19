package rs.raf.demo.responses;

import lombok.Data;

@Data
public class ResponseDto {
    private String response;
    private Integer responseCode;

    public ResponseDto(String response, Integer responseCode) {
        this.response = response;
        this.responseCode = responseCode;
    }

}
