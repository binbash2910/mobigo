import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse, provideHttpClient } from '@angular/common/http';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, from, of } from 'rxjs';

import { IUser } from 'app/entities/user/user.model';
import { UserService } from 'app/entities/user/service/user.service';
import { PeopleService } from '../service/people.service';
import { IPeople } from '../people.model';
import { PeopleFormService } from './people-form.service';

import { PeopleUpdateComponent } from './people-update.component';

describe('People Management Update Component', () => {
  let comp: PeopleUpdateComponent;
  let fixture: ComponentFixture<PeopleUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let peopleFormService: PeopleFormService;
  let peopleService: PeopleService;
  let userService: UserService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PeopleUpdateComponent],
      providers: [
        provideHttpClient(),
        FormBuilder,
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    })
      .overrideTemplate(PeopleUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(PeopleUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    peopleFormService = TestBed.inject(PeopleFormService);
    peopleService = TestBed.inject(PeopleService);
    userService = TestBed.inject(UserService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('should call User query and add missing value', () => {
      const people: IPeople = { id: 20275 };
      const user: IUser = { id: 3944 };
      people.user = user;

      const userCollection: IUser[] = [{ id: 3944 }];
      jest.spyOn(userService, 'query').mockReturnValue(of(new HttpResponse({ body: userCollection })));
      const additionalUsers = [user];
      const expectedCollection: IUser[] = [...additionalUsers, ...userCollection];
      jest.spyOn(userService, 'addUserToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ people });
      comp.ngOnInit();

      expect(userService.query).toHaveBeenCalled();
      expect(userService.addUserToCollectionIfMissing).toHaveBeenCalledWith(
        userCollection,
        ...additionalUsers.map(expect.objectContaining),
      );
      expect(comp.usersSharedCollection).toEqual(expectedCollection);
    });

    it('should update editForm', () => {
      const people: IPeople = { id: 20275 };
      const user: IUser = { id: 3944 };
      people.user = user;

      activatedRoute.data = of({ people });
      comp.ngOnInit();

      expect(comp.usersSharedCollection).toContainEqual(user);
      expect(comp.people).toEqual(people);
    });
  });

  describe('save', () => {
    it('should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IPeople>>();
      const people = { id: 9353 };
      jest.spyOn(peopleFormService, 'getPeople').mockReturnValue(people);
      jest.spyOn(peopleService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ people });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: people }));
      saveSubject.complete();

      // THEN
      expect(peopleFormService.getPeople).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(peopleService.update).toHaveBeenCalledWith(expect.objectContaining(people));
      expect(comp.isSaving).toEqual(false);
    });

    it('should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IPeople>>();
      const people = { id: 9353 };
      jest.spyOn(peopleFormService, 'getPeople').mockReturnValue({ id: null });
      jest.spyOn(peopleService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ people: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: people }));
      saveSubject.complete();

      // THEN
      expect(peopleFormService.getPeople).toHaveBeenCalled();
      expect(peopleService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IPeople>>();
      const people = { id: 9353 };
      jest.spyOn(peopleService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ people });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(peopleService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareUser', () => {
      it('should forward to userService', () => {
        const entity = { id: 3944 };
        const entity2 = { id: 6275 };
        jest.spyOn(userService, 'compareUser');
        comp.compareUser(entity, entity2);
        expect(userService.compareUser).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
