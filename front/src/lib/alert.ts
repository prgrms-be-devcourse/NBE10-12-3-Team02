import Swal from "sweetalert2";

// 서비스 시그니처 컬러(블루)를 버튼 색으로 통일해서 쓴다.
const THEME_COLOR = "#2563eb";

// SweetAlert 기본 디자인 대신, 앱 안에 이미 있는 커스텀 모달(회원탈퇴 확인창 등)과
// 똑같은 생김새(둥근 흰 카드, 진한 제목, 회색 설명, 파란/회색 버튼)로 맞춘다.
// buttonsStyling: false로 SweetAlert 기본 버튼 스타일을 끄고, 우리 Tailwind 클래스로 직접 그린다.
const baseOptions = {
    width: 420,
    buttonsStyling: false,
    reverseButtons: true,
    customClass: {
        popup: "rounded-2xl! p-3!",
        icon: "scale-80! mt-2! mb-1!",
        title: "text-xl! font-bold! text-gray-800! mt-1! mb-0!",
        htmlContainer: "text-gray-600! text-base! mt-1! mb-5!",
        actions: "gap-3! w-full! px-3! mt-0!",
        confirmButton:
            "flex-1 p-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-bold transition",
        cancelButton:
            "flex-1 p-3 bg-gray-100 hover:bg-gray-200 text-gray-700 rounded-lg font-bold transition",
    },
};

// 위험한 동작(탈퇴, 취소 등)일 때는 확인 버튼만 빨간색으로 바꾼다.
const dangerConfirmButton =
    "flex-1 p-3 bg-red-500 hover:bg-red-600 text-white rounded-lg font-bold transition";

// 기존 alert(message) 대체용. await 하면 사용자가 확인 누른 뒤 다음 코드가 실행된다.
export function showAlert(message: string, title?: string) {
    return Swal.fire({
        ...baseOptions,
        icon: "info",
        iconColor: THEME_COLOR,
        title,
        text: message,
        confirmButtonText: "확인",
    });
}

// 성공 메시지 전용 (체크 아이콘)
export function showSuccess(message: string, title = "완료") {
    return Swal.fire({
        ...baseOptions,
        icon: "success",
        title,
        text: message,
        confirmButtonText: "확인",
    });
}

// 에러 메시지 전용 (에러 아이콘)
export function showError(message: string, title = "오류") {
    return Swal.fire({
        ...baseOptions,
        icon: "error",
        title,
        text: message,
        confirmButtonText: "확인",
    });
}

// 기존 confirm(message) 대체용. 사용자가 "확인"을 눌렀으면 true, "취소"나 바깥 클릭이면 false.
export async function showConfirm(
    message: string,
    options?: { title?: string; confirmText?: string; cancelText?: string; danger?: boolean },
): Promise<boolean> {
    const result = await Swal.fire({
        ...baseOptions,
        icon: options?.danger ? "warning" : "question",
        iconColor: options?.danger ? "#dc2626" : THEME_COLOR,
        title: options?.title,
        text: message,
        showCancelButton: true,
        confirmButtonText: options?.confirmText ?? "확인",
        cancelButtonText: options?.cancelText ?? "취소",
        customClass: {
            ...baseOptions.customClass,
            confirmButton: options?.danger ? dangerConfirmButton : baseOptions.customClass.confirmButton,
        },
    });
    return result.isConfirmed;
}