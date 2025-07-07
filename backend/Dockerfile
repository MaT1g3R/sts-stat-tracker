# Build-Stage
FROM golang:1.24-alpine AS build
WORKDIR /app

# Install templ
RUN go install github.com/a-h/templ/cmd/templ@latest

# Install esbuild
RUN apk add --no-cache esbuild

# Copy the source code
COPY . .

# Generate templ files
RUN templ generate

# Generate js bundle
RUN cat assets/js/src/* | esbuild --minify --bundle --outfile=assets/js/main.min.js

# Build the application
RUN CGO_ENABLED=0 GOOS=linux go build -o main ./main.go

# Deploy-Stage
FROM alpine:3.20.2
WORKDIR /app

# Install ca-certificates
RUN apk add --no-cache ca-certificates

# Set environment variable for runtime
ENV GO_ENV=production

# Copy the binary from the build stage
COPY --from=build /app/main .
COPY --from=build /app/assets ./assets

# Expose the port your application runs on
EXPOSE 8090

# Command to run the application
CMD ["./main"]