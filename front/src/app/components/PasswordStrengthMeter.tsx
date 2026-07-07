"use client";

import {
  getPasswordStrength,
  STRENGTH_LABEL,
  STRENGTH_BAR_COLOR,
  STRENGTH_TEXT_COLOR,
  STRENGTH_LEVELS,
} from "@/lib/passwordStrength";

export default function PasswordStrengthMeter({ password }: { password: string }) {
  const strength = getPasswordStrength(password);
  if (!strength) return null;

  const currentIndex = STRENGTH_LEVELS.indexOf(strength);

  return (
    <div className="mb-3 -mt-1">
      <div className="flex gap-1 h-1">
        {STRENGTH_LEVELS.map((level, i) => (
          <div
            key={level}
            className={`flex-1 rounded ${i <= currentIndex ? STRENGTH_BAR_COLOR[strength] : "bg-gray-200"}`}
          />
        ))}
      </div>
      <p className={`text-xs mt-1 ${STRENGTH_TEXT_COLOR[strength]}`}>
        비밀번호 강도: {STRENGTH_LABEL[strength]}
      </p>
    </div>
  );
}