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
            new SeatGrade("R", 120000, "B"),
            new SeatGrade("R", 120000, "C"),
            new SeatGrade("S", 90000, "D"),
            new SeatGrade("S", 90000, "E"),
            new SeatGrade("S", 90000, "F"),
            new SeatGrade("A", 70000, "G"),
            new SeatGrade("A", 70000, "H"),
            new SeatGrade("A", 70000, "I"),
            new SeatGrade("A", 70000, "J")
    );

    private static final int SEATS_PER_ROW = 60;

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
