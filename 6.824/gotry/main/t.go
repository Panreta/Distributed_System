package main
import (
	"fmt" // format the I/O
	"strings"
)

type KeyValue struct {
	Key   string
	Value string
}

func MapReduce(input []KeyValue, mapFunc func(string, string) []KeyValue, 
               reduceFunc func(string, []string) string) []KeyValue {

	intermediate := []KeyValue{} // intermediate key-value pairs
	// example of intermediate: []KeyValue{{"word1", "1"}, {"word2", "1"}, ...}

	for _, kv := range input {
		intermediate = append(intermediate, mapFunc(kv.Key, kv.Value)...)
	}
	
	// Shuffle
	shuffled := make(map[string][]string) //make here means to create a map
	// shuffled here means: input: word -> list of "1"s
	// example of input in shuffled: map[string][]string{"word1": {"1", "1"}, "word2": {"1"}, ...}
	for _, kv := range intermediate {
		shuffled[kv.Key] = append(shuffled[kv.Key], kv.Value)
	}
	
	// Reduce
	result := []KeyValue{}
	for key, values := range shuffled {
		result = append(result, KeyValue{key, reduceFunc(key, values)})
	}
	return result
}

func main() {
	input := []KeyValue{
		{"doc1", "hello world hello"},
		{"doc2", "world of go"},
	}
	
	// Map: split words
	mapFunc := func(key, value string) []KeyValue {
		kvs := []KeyValue{}
		for _, word := range strings.Fields(value) {
			kvs = append(kvs, KeyValue{word, "1"})
		}
		return kvs
	}
	
	// Reduce: count
	reduceFunc := func(key string, values []string) string {
		return fmt.Sprintf("%d", len(values))
	}
	
	result := MapReduce(input, mapFunc, reduceFunc)
	
	for _, kv := range result {
		fmt.Printf("%s: %s\n", kv.Key, kv.Value)
	}
}