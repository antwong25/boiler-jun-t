package org.example.boilerpojo;

import lombok.Data;

import java.util.List;

@Data
public class CreditScoreRecalculateVO {
    private Integer totalUserCount;
    private Integer updatedUserCount;
    private List<UserVO> users;
}
