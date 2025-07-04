package stats

import (
	"fmt"
	"math"
)

// FormatTime convert seconds to a human-readable format
func FormatTime(seconds int) string {
	secs := seconds % 60
	duration := seconds / 60
	minutes := duration % 60
	hours := seconds / 3600

	if hours > 0 {
		return fmt.Sprintf("%02dh %02dm %02ds", hours, minutes, secs)
	}
	return fmt.Sprintf("%02dm %02ds", minutes, secs)

}

// FormatPercentage formats a float as a percentage with 1 decimal place
func FormatPercentage(value float64) string {
	if math.IsNaN(value) || math.IsInf(value, 0) {
		return "0.0%"
	}
	return fmt.Sprintf("%.1f%%", value*100)
}
