import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IUser } from 'app/entities/user/user.model';
import { UserService } from 'app/entities/user/service/user.service';
import { IPeople } from '../people.model';
import { PeopleService } from '../service/people.service';
import { PeopleFormGroup, PeopleFormService } from './people-form.service';

@Component({
  selector: 'jhi-people-update',
  templateUrl: './people-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class PeopleUpdateComponent implements OnInit {
  isSaving = false;
  people: IPeople | null = null;

  usersSharedCollection: IUser[] = [];

  protected peopleService = inject(PeopleService);
  protected peopleFormService = inject(PeopleFormService);
  protected userService = inject(UserService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: PeopleFormGroup = this.peopleFormService.createPeopleFormGroup();

  compareUser = (o1: IUser | null, o2: IUser | null): boolean => this.userService.compareUser(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ people }) => {
      this.people = people;
      if (people) {
        this.updateForm(people);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const people = this.peopleFormService.getPeople(this.editForm);
    if (people.id !== null) {
      this.subscribeToSaveResponse(this.peopleService.update(people));
    } else {
      this.subscribeToSaveResponse(this.peopleService.create(people));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IPeople>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(people: IPeople): void {
    this.people = people;
    this.peopleFormService.resetForm(this.editForm, people);

    this.usersSharedCollection = this.userService.addUserToCollectionIfMissing<IUser>(this.usersSharedCollection, people.user);
  }

  protected loadRelationshipsOptions(): void {
    this.userService
      .query()
      .pipe(map((res: HttpResponse<IUser[]>) => res.body ?? []))
      .pipe(map((users: IUser[]) => this.userService.addUserToCollectionIfMissing<IUser>(users, this.people?.user)))
      .subscribe((users: IUser[]) => (this.usersSharedCollection = users));
  }
}
