package ru.kpfu.itis.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ConnectPlayerMessage {

    private String nickName;
    private int indexOfPlayer;

}