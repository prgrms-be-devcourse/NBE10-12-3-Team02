package com.back.global.initData.generator;

import com.back.domain.schedule.entity.Schedule;
import com.back.domain.schedule.entity.ScheduleSeat;
import com.back.domain.schedule.entity.SeatStatus;

import java.util.ArrayList;
import java.util.List;

public class SeatGenerator {

    private record SeatGrade(String gradeName, int price, String rowLabel) {}

    private static final List<SeatGrade> GRADES = List.of(
            new SeatGrade("VIP", 150000, "A"),
            new SeatGrade("VIP", 150000, "B"),
            new SeatGrade("R", 120000, "C"),
            new SeatGrade("R", 120000, "D"),
            new SeatGrade("R", 120000, "E"),
            new SeatGrade("R", 120000, "F"),
            new SeatGrade("S", 90000, "G"),
            new SeatGrade("S", 90000, "H"),
            new SeatGrade("S", 90000, "I"),
            new SeatGrade("S", 90000, "J"),
            new SeatGrade("S", 90000, "K"),
            new SeatGrade("S", 90000, "L"),
            new SeatGrade("A", 70000, "M"),
            new SeatGrade("A", 70000, "N"),
            new SeatGrade("A", 70000, "O"),
            new SeatGrade("A", 70000, "P"),
            new SeatGrade("A", 70000, "Q"),
            new SeatGrade("A", 70000, "R"),
            new SeatGrade("A", 70000, "S"),
            new SeatGrade("A", 70000, "T")
    );

    private static final int SEATS_PER_ROW = 30;

    public static List<ScheduleSeat> generate(Schedule schedule) {
        List<ScheduleSeat> seats = new ArrayList<>();
        for (SeatGrade grade : GRADES) {
            for (int num = 1; num <= SEATS_PER_ROW; num++) {
                String seatNumber = grade.rowLabel() + "-" + num;
                seats.add(ScheduleSeat.create(
                        schedule,
                        grade.gradeName(),
                        seatNumber,
                        grade.price(),
                        SeatStatus.AVAILABLE
                ));
            }
        }
        return seats;
    }
}
