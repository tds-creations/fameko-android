FROM golang:1.22-alpine AS build
WORKDIR /app
COPY go.mod go.sum ./
# Allow Go to download the required toolchain if needed
ENV GOTOOLCHAIN=auto
RUN go mod download
COPY . .
RUN go build -o main .

FROM alpine:latest
WORKDIR /app
COPY --from=build /app/main .
EXPOSE $PORT
CMD ["./main"]
