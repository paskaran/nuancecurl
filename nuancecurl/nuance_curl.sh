curl $1"?appId="$2"&appKey="$3"&id="$4 -H "Content-Type: "$5 -H "Accept-Language: "$6 -H "Transfer-Encoding: chunked" -H "Accept: application/xml" -H "X-Dictation-EscapeNewLine: true" -H "Accept-Topic: "$7 -k -s --data-binary @$8 
 
