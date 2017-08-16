NAME = final_report
TEX = $(NAME).tex
PDF = $(NAME).pdf

$(PDF): $(TEX)
	latexmk -pdf $(TEX)


.PHONY: all, clean, distclean
all: $(PDF)

clean:
	rm -rf *.toc *.bbl *.nav *.out *.log *.aux *.blg *-fig*.tex *.dot *.fls *.fdb_latexmk

distclean: clean
	rm -rf *.pdf
