export type PasswordStrength = "weak" | "medium" | "strong";

export function getPasswordStrength(password: string): PasswordStrength | null {
  if (password.length === 0) return null;
  if (password.length < 8) return "weak";

  let score = 0;
  if (password.length >= 12) score++;
  if (/[a-z]/.test(password)) score++;
  if (/[A-Z]/.test(password)) score++;
  if (/[0-9]/.test(password)) score++;
  if (/[^a-zA-Z0-9]/.test(password)) score++;

  if (score <= 2) return "weak";
  if (score <= 3) return "medium";
  return "strong";
}

export const STRENGTH_LEVELS: PasswordStrength[] = ["weak", "medium", "strong"];

export const STRENGTH_LABEL: Record<PasswordStrength, string> = {
  weak: "약함",
  medium: "보통",
  strong: "강함",
};

export const STRENGTH_BAR_COLOR: Record<PasswordStrength, string> = {
  weak: "bg-red-400",
  medium: "bg-yellow-400",
  strong: "bg-green-500",
};

export const STRENGTH_TEXT_COLOR: Record<PasswordStrength, string> = {
  weak: "text-red-500",
  medium: "text-yellow-600",
  strong: "text-green-600",
};