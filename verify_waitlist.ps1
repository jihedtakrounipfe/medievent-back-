# Verification Script for Kafka Waitlist
$baseUrl = "http://localhost:8082/mediconnect/api/v1"

# 1. Get Event ID (Assuming it's the one created by DataInitializer)
Write-Host "Fetching events..."
$events = Invoke-RestMethod -Uri "$baseUrl/events/active" -Method Get
$event = $events | Where-Object { $_.title -eq "Sommet MediConnect 2026" }
$eventId = $event.id
Write-Host "Found Event ID: $eventId"

# 2. Check current status of takrounipc@gmail.com
# Note: We need a token to check status or we can check via database
# Since I am the AI, I will just trust the DataInitializer logs for now, or check via simple endpoint if available.
# Actually, let's use the DB to verify.

Write-Host "Verifying initial status in DB..."
docker exec MediConnectMySQL mysql -u root -proot123 mediconnect_db -e "SELECT u.email, ep.status FROM event_participants ep JOIN users u ON ep.user_id = u.id WHERE ep.event_id = $eventId;"

# 3. Get Token for user1@test.com
Write-Host "Getting token for user1@test.com..."
$tokenResponse = Invoke-RestMethod -Uri "http://localhost:9090/realms/mediconnect-main/protocol/openid-connect/token" `
    -Method Post `
    -ContentType "application/x-www-form-urlencoded" `
    -Body @{
        grant_type = "password"
        client_id = "angular-spa"
        username = "user1@test.com"
        password = "Password123"
    }
$token = $tokenResponse.access_token

# 4. Cancel participation for user1@test.com via API
Write-Host "Simulating cancellation for user1@test.com via API..."
$headers = @{ "Authorization" = "Bearer $token" }
Invoke-RestMethod -Uri "$baseUrl/events/participation/$eventId/cancel" -Method Delete -Headers $headers

Write-Host "Waiting 10 seconds for Kafka promotion..."
Start-Sleep -Seconds 10

# 5. Check final status
Write-Host "Verifying final status in DB..."
docker exec MediConnectMySQL mysql -u root -proot123 mediconnect_db -e "SELECT u.email, ep.status FROM event_participants ep JOIN users u ON ep.user_id = u.id WHERE ep.event_id = $eventId;"
