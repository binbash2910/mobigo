import { Component, OnInit, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import SharedModule from 'app/shared/shared.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { IRide } from 'app/entities/ride/ride.model';
import { RideService } from 'app/entities/ride/service/ride.service';
import { IPeople } from 'app/entities/people/people.model';
import { PeopleService } from 'app/entities/people/service/people.service';
import { RatingService } from '../service/rating.service';
import { IRating } from '../rating.model';
import { RatingFormGroup, RatingFormService } from './rating-form.service';

@Component({
  selector: 'jhi-rating-update',
  templateUrl: './rating-update.component.html',
  imports: [SharedModule, FormsModule, ReactiveFormsModule],
})
export class RatingUpdateComponent implements OnInit {
  isSaving = false;
  rating: IRating | null = null;

  ridesSharedCollection: IRide[] = [];
  peopleSharedCollection: IPeople[] = [];

  protected ratingService = inject(RatingService);
  protected ratingFormService = inject(RatingFormService);
  protected rideService = inject(RideService);
  protected peopleService = inject(PeopleService);
  protected activatedRoute = inject(ActivatedRoute);

  // eslint-disable-next-line @typescript-eslint/member-ordering
  editForm: RatingFormGroup = this.ratingFormService.createRatingFormGroup();

  compareRide = (o1: IRide | null, o2: IRide | null): boolean => this.rideService.compareRide(o1, o2);

  comparePeople = (o1: IPeople | null, o2: IPeople | null): boolean => this.peopleService.comparePeople(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ rating }) => {
      this.rating = rating;
      if (rating) {
        this.updateForm(rating);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const rating = this.ratingFormService.getRating(this.editForm);
    if (rating.id !== null) {
      this.subscribeToSaveResponse(this.ratingService.update(rating));
    } else {
      this.subscribeToSaveResponse(this.ratingService.create(rating));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IRating>>): void {
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

  protected updateForm(rating: IRating): void {
    this.rating = rating;
    this.ratingFormService.resetForm(this.editForm, rating);

    this.ridesSharedCollection = this.rideService.addRideToCollectionIfMissing<IRide>(this.ridesSharedCollection, rating.trajet);
    this.peopleSharedCollection = this.peopleService.addPeopleToCollectionIfMissing<IPeople>(
      this.peopleSharedCollection,
      rating.passager,
      rating.conducteur,
    );
  }

  protected loadRelationshipsOptions(): void {
    this.rideService
      .query()
      .pipe(map((res: HttpResponse<IRide[]>) => res.body ?? []))
      .pipe(map((rides: IRide[]) => this.rideService.addRideToCollectionIfMissing<IRide>(rides, this.rating?.trajet)))
      .subscribe((rides: IRide[]) => (this.ridesSharedCollection = rides));

    this.peopleService
      .query()
      .pipe(map((res: HttpResponse<IPeople[]>) => res.body ?? []))
      .pipe(
        map((people: IPeople[]) =>
          this.peopleService.addPeopleToCollectionIfMissing<IPeople>(people, this.rating?.passager, this.rating?.conducteur),
        ),
      )
      .subscribe((people: IPeople[]) => (this.peopleSharedCollection = people));
  }
}
