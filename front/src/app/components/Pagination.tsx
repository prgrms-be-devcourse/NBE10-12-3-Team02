interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  basePage?: 0 | 1;
  className?: string;
}

export default function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  basePage = 1,
  className = "flex items-center justify-center gap-2 mt-8",
}: PaginationProps) {
  if (totalPages <= 1) return null;

  const firstPage = basePage;
  const lastPage = basePage + totalPages - 1;

  return (
    <div className={className}>
      <button
        type="button"
        onClick={() => onPageChange(Math.max(firstPage, currentPage - 1))}
        disabled={currentPage === firstPage}
        className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
      >
        이전
      </button>
      {Array.from({ length: totalPages }, (_, i) => firstPage + i).map(
        (page) => (
          <button
            type="button"
            key={page}
            onClick={() => onPageChange(page)}
            className={`w-10 h-10 rounded-lg border text-sm font-semibold ${
              currentPage === page
                ? "bg-blue-600 border-blue-600 text-white"
                : "bg-white border-gray-200 text-gray-600 hover:bg-gray-50"
            }`}
          >
            {page - basePage + 1}
          </button>
        ),
      )}
      <button
        type="button"
        onClick={() => onPageChange(Math.min(lastPage, currentPage + 1))}
        disabled={currentPage === lastPage}
        className="px-3 py-2 rounded-lg border border-gray-200 bg-white text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-default"
      >
        다음
      </button>
    </div>
  );
}
