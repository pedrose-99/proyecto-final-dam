import { Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { User } from "../models/user.model";

@Injectable({
    providedIn: "root"
})
export class UserService 
{
    private apiUrl = environment.apiUrl;

	constructor(private http: HttpClient) 
    {    
    }

	getProfile(): Observable<User> 
	{
		return this.http.get<User>(`${this.apiUrl}/users/profile`);
	}

	updateProfile(data: { username?: string; newPassword?: string; }): Observable<User>
	{
		return this.http.put<User>(`${this.apiUrl}/users/me`, data);
	}
}